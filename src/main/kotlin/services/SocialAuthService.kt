package com.example.services

import com.example.exceptions.ApiException
import com.example.models.FCMTokenRequest
import com.example.models.dto.UserDto
import com.example.models.dto.SocialLoginRequest
import com.example.models.dto.LinkAppleRelayRequest
import com.example.models.dto.UserLoginResponse
import com.example.repositories.TenantFeaturesRepository
import com.example.repositories.UserPermissionsRepository
import com.example.repositories.UserRepository
import com.example.tenant.TenantContextHolder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseToken
import io.ktor.http.*
import java.util.*

class SocialAuthService(
    private val userRepository: UserRepository,
    private val fcmService: FCMService?,
    private val tenantFeaturesRepository: TenantFeaturesRepository? = null,
    private val userPermissionsRepository: UserPermissionsRepository? = null
) {

    /**
     * Authenticate user via social sign-in (Google/Apple).
     *
     * Flow:
     * 1. Verify the Firebase ID token
     * 2. Extract email from the verified token
     * 3. Look up user by email in the tenant's Users table
     * 4. If found → return login response (same as password login)
     * 5. If NOT found → reject (no self-registration)
     */
    suspend fun authenticateWithSocial(request: SocialLoginRequest): UserLoginResponse {
        // Step 1: Verify Firebase ID token
        val firebaseToken = verifyIdToken(request.idToken)

        // Step 2: Extract email
        val email = firebaseToken.email
            ?: throw ApiException(
                "No email associated with this ${request.provider} account. Please use an account with an email address.",
                HttpStatusCode.BadRequest
            )

        // Step 3: Look up user by email (handle Apple private relay)
        var users = userRepository.findByEmail(email)

        // If not found and it's an Apple private relay email, check relay column and real email
        if (users.isNullOrEmpty() && email.endsWith("@privaterelay.appleid.com")) {
            // First check if relay email is already linked
            users = userRepository.findByAppleRelayEmail(email)

            // If still not found, try the real email from client (available on first auth)
            if (users.isNullOrEmpty()) {
                val realEmail = request.appleRealEmail
                if (!realEmail.isNullOrBlank()) {
                    users = userRepository.findByEmail(realEmail)
                    // Auto-link relay email if user found by real email
                    if (!users.isNullOrEmpty()) {
                        val userId = UUID.fromString(users[0].id)
                        userRepository.linkAppleRelayEmail(userId, email)
                        println("[Social Auth] Auto-linked Apple relay email $email to user ${users[0].email}")
                    }
                }
            }

            if (users.isNullOrEmpty()) {
                throw ApiException(
                    "No account found with this Apple ID. Your email uses Apple's Hide My Email feature. Please link your account.|APPLE_RELAY|$email",
                    HttpStatusCode.NotFound
                )
            }
        }

        if (users.isNullOrEmpty()) {
            throw ApiException(
                "No account found with this email ($email). Please contact your administrator.",
                HttpStatusCode.NotFound
            )
        }

        // Step 4: Update FCM token if provided
        if (request.fcmToken != null && request.platform != null && fcmService != null) {
            try {
                updateFCMTokenOnSocialLogin(
                    userId = UUID.fromString(users[0].id),
                    fcmToken = request.fcmToken,
                    deviceId = request.deviceId,
                    platform = request.platform
                )
            } catch (e: Exception) {
                println("Warning: Failed to update FCM token for user ${users[0].id}: ${e.message}")
            }
        }

        // Step 5: Get enabled features
        val enabledFeatures = getEnabledFeaturesForUser(users[0])

        return UserLoginResponse(
            user = users,
            token = null,
            enabledFeatures = enabledFeatures
        )
    }

    /**
     * Verify Firebase ID token using Firebase Admin SDK.
     * Throws ApiException if token is invalid or expired.
     */
    private fun verifyIdToken(idToken: String): FirebaseToken {
        return try {
            FirebaseAuth.getInstance().verifyIdToken(idToken)
        } catch (e: Exception) {
            println("Firebase token verification failed: ${e.message}")
            throw ApiException(
                "Invalid or expired authentication token. Please try signing in again.",
                HttpStatusCode.Unauthorized
            )
        }
    }

    private suspend fun getEnabledFeaturesForUser(userDto: UserDto): List<String>? {
        try {
            val tenantContext = TenantContextHolder.getTenant()
            val tenantId = tenantContext?.id ?: return null

            return when (userDto.role.uppercase()) {
                "ADMIN", "STUDENT" -> {
                    tenantFeaturesRepository?.getEnabledFeatureKeys(tenantId) ?: emptyList()
                }
                "STAFF" -> {
                    val userId = UUID.fromString(userDto.id)
                    userPermissionsRepository?.getEnabledFeatureKeys(userId) ?: emptyList()
                }
                else -> null
            }
        } catch (e: Exception) {
            println("Warning: Failed to fetch enabled features for user ${userDto.id}: ${e.message}")
            return null
        }
    }

    private suspend fun updateFCMTokenOnSocialLogin(
        userId: UUID,
        fcmToken: String,
        deviceId: String?,
        platform: String
    ) {
        if (fcmToken.isBlank() || platform.isBlank()) return

        val tokenRequest = FCMTokenRequest(
            token = fcmToken,
            deviceId = deviceId,
            platform = platform
        )

        fcmService?.saveToken(userId, tokenRequest)
    }

    /**
     * Link an Apple private relay email to an existing user account.
     * User must verify identity with their actual email + password.
     */
    suspend fun linkAppleRelayEmail(request: LinkAppleRelayRequest): UserLoginResponse {
        // Step 1: Find user by actual email
        val users = userRepository.findByEmail(request.actualEmail)
        if (users.isNullOrEmpty()) {
            throw ApiException(
                "No account found with this email (${request.actualEmail}). Please check your email.",
                HttpStatusCode.NotFound
            )
        }

        // Step 2: Verify password
        val passwordHash = userRepository.findPasswordHashByEmail(request.actualEmail)
            ?: throw ApiException("Unable to verify credentials", HttpStatusCode.Unauthorized)

        if (!org.mindrot.jbcrypt.BCrypt.checkpw(request.password, passwordHash)) {
            throw ApiException("Invalid password", HttpStatusCode.Unauthorized)
        }

        // Step 3: Link the relay email to the user
        val userId = UUID.fromString(users[0].id)
        val linked = userRepository.linkAppleRelayEmail(userId, request.relayEmail)
        if (!linked) {
            throw ApiException("Failed to link Apple account", HttpStatusCode.InternalServerError)
        }

        println("[Social Auth] Linked Apple relay email ${request.relayEmail} to user ${request.actualEmail}")

        // Step 4: Update FCM token if provided
        if (request.fcmToken != null && request.platform != null && fcmService != null) {
            try {
                updateFCMTokenOnSocialLogin(
                    userId = userId,
                    fcmToken = request.fcmToken,
                    deviceId = request.deviceId,
                    platform = request.platform
                )
            } catch (e: Exception) {
                println("Warning: Failed to update FCM token: ${e.message}")
            }
        }

        // Step 5: Return login response
        val enabledFeatures = getEnabledFeaturesForUser(users[0])

        return UserLoginResponse(
            user = users,
            token = null,
            enabledFeatures = enabledFeatures
        )
    }
}
