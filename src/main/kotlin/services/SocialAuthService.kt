package com.example.services

import com.example.exceptions.ApiException
import com.example.models.FCMTokenRequest
import com.example.models.dto.UserDto
import com.example.models.dto.SocialLoginRequest
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

        // Step 3: Look up user by email
        val users = userRepository.findByEmail(email)
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
}
