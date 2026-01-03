package com.example.services

import com.example.database.tables.UserRole
import com.example.exceptions.ApiException
import com.example.models.FCMTokenRequest
import com.example.models.dto.*
import com.example.repositories.TenantFeaturesRepository
import com.example.repositories.UserPermissionsRepository
import com.example.repositories.UserRepository
import com.example.tenant.TenantContextHolder
import io.ktor.http.*
import org.mindrot.jbcrypt.BCrypt
import services.S3FileService
import java.io.InputStream
import java.util.*

class UserService(
    private val userRepository: UserRepository,
    private val fcmService: FCMService?,
    private val tenantFeaturesRepository: TenantFeaturesRepository? = null,
    private val userPermissionsRepository: UserPermissionsRepository? = null,
    private val s3FileService: S3FileService? = null,
    private val otpRepository: com.example.repositories.OtpRepository? = null,
    private val emailService: EmailService? = null
) {

    suspend fun createUser(request: CreateUserRequest): UserDto {
        validateCreateUserRequest(request)

        // Check if email already exists
        if (userRepository.emailExists(request.email)) {
            throw ApiException("Email already exists", HttpStatusCode.Conflict)
        }

        // Hash password
        val hashedPassword = BCrypt.hashpw(request.password, BCrypt.gensalt())

        val userId = userRepository.create(request, hashedPassword)
        return getUserById(userId)
    }

    suspend fun getUserById(id: UUID): UserDto {
        return userRepository.findById(id)
            ?: throw ApiException("User not found", HttpStatusCode.NotFound)
    }

    suspend fun getUserById(id: String): UserDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }
        return getUserById(uuid)
    }

    suspend fun getUserByEmail(email: String): List<UserDto> {
        return userRepository.findByEmail(email)
            ?: throw ApiException("User not found", HttpStatusCode.NotFound)
    }

    suspend fun getAdminUsers(): List<UserDto> {
        return userRepository.findByAdminType()
            ?: throw ApiException("User not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllUsers(): List<UserDto> {
        return userRepository.findAll()
    }

    suspend fun getUsersByRole(roleString: String): List<UserDto> {
        val role = try {
            UserRole.valueOf(roleString.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid role: $roleString", HttpStatusCode.BadRequest)
        }

        return userRepository.findByRole(role)
    }

    suspend fun getUsersWithFilters(
        role: String? = null,
        classId: String? = null,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): com.example.models.responses.PaginatedResponse<List<com.example.models.dto.UserWithDetailsDto>> {
        // Validate pagination parameters
        val validPage = if (page < 1) 1 else page
        val validPageSize = when {
            pageSize < 1 -> 20
            pageSize > 100 -> 100  // Max 100 items per page
            else -> pageSize
        }

        // Get total count
        val totalItems = userRepository.countUsersWithFilters(role, classId, search)
        val totalPages = if (totalItems == 0L) 0L else ((totalItems + validPageSize - 1) / validPageSize)

        // Get paginated data
        val users = userRepository.findUsersWithFilters(role, classId, search, validPage, validPageSize)

        // Build pagination info
        val paginationInfo = com.example.models.responses.PaginatedResponse.PaginationInfo(
            page = validPage,
            pageSize = validPageSize,
            totalItems = totalItems,
            totalPages = totalPages,
            hasNext = validPage < totalPages,
            hasPrevious = validPage > 1
        )

        return com.example.models.responses.PaginatedResponse(
            success = true,
            data = users,
            pagination = paginationInfo
        )
    }

    suspend fun updateUser(id: String, request: UpdateUserRequest): UserDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        validateUpdateUserRequest(request)

        // Check if email already exists for other users
        if (userRepository.emailExistsForOtherUser(request.email, uuid)) {
            throw ApiException("Email already exists", HttpStatusCode.Conflict)
        }

        val updated = userRepository.update(uuid, request)
        if (!updated) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }

        return getUserById(uuid)
    }

    suspend fun changePassword(id: String, request: ChangePasswordRequest): UserDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        validateChangePasswordRequest(request)

        // Get current user to verify current password
        val user = getUserById(uuid)
        val currentPasswordHash = userRepository.findPasswordHashByEmail(user.email)
            ?: throw ApiException("User not found", HttpStatusCode.NotFound)

        // Verify current password
        if (!BCrypt.checkpw(request.currentPassword, currentPasswordHash)) {
            throw ApiException("Current password is incorrect", HttpStatusCode.BadRequest)
        }

        // Hash new password
        val newHashedPassword = BCrypt.hashpw(request.newPassword, BCrypt.gensalt())

        val updated = userRepository.updatePassword(uuid, newHashedPassword)
        if (!updated) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }

        return getUserById(uuid)
    }

    suspend fun deleteUser(id: String) {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        val deleted = userRepository.delete(uuid)
        if (!deleted) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun uploadUserPhoto(
        userId: String,
        tenantId: String,
        inputStream: InputStream,
        originalFileName: String
    ): UserDto {
        val uuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        // Verify user exists
        val user = getUserById(uuid)

        // Check if S3 service is available
        if (s3FileService == null) {
            throw ApiException("S3 file service not available", HttpStatusCode.ServiceUnavailable)
        }

        // Upload photo to S3
        val uploadResponse = s3FileService.uploadProfilePicture(
            tenantId = tenantId,
            inputStream = inputStream,
            originalFileName = originalFileName,
            userId = userId
        )

        if (!uploadResponse.success) {
            throw ApiException(
                uploadResponse.message ?: "Failed to upload photo",
                HttpStatusCode.InternalServerError
            )
        }

        // Update user record with imageUrl and imageS3Key
        val updateRequest = UpdateUserRequest(
            email = user.email,
            mobileNumber = user.mobileNumber,
            role = user.role,
            firstName = user.firstName,
            lastName = user.lastName,
            photoUrl = user.photoUrl, // Keep existing photoUrl
            imageUrl = uploadResponse.fileUrl, // Set new S3 URL
            imageS3Key = uploadResponse.objectKey // Set S3 object key
        )

        userRepository.update(uuid, updateRequest)

        return getUserById(uuid)
    }

    suspend fun authenticateUser(request: UserLoginRequest): UserLoginResponse {
        validateLoginRequest(request)

        val user = userRepository.findByMobile(request.mobileNumber)
            ?: throw ApiException("Invalid email or password", HttpStatusCode.Unauthorized)

        val passwordHash = userRepository.findPasswordHashByMobile(request.mobileNumber)
            ?: throw ApiException("Invalid email or password", HttpStatusCode.Unauthorized)

        if (!BCrypt.checkpw(request.password, passwordHash)) {
            throw ApiException("Invalid email or password", HttpStatusCode.Unauthorized)
        }

        // Get enabled features based on user role
        val enabledFeatures = getEnabledFeaturesForUser(user[0])

        return UserLoginResponse(
            user = user,
            token = null, // Implement JWT token generation here if needed
            enabledFeatures = enabledFeatures
        )
    }

    private suspend fun getEnabledFeaturesForUser(userDto: UserDto): List<String>? {
        try {
            val tenantContext = TenantContextHolder.getTenant()
            val tenantId = tenantContext?.id ?: return null

            return when (userDto.role.uppercase()) {
                "ADMIN", "STUDENT" -> {
                    // ADMIN gets all tenant's enabled features
                    tenantFeaturesRepository?.getEnabledFeatureKeys(tenantId) ?: emptyList()
                }
                "STAFF" -> {
                    // STAFF gets only their assigned features
                    val userId = UUID.fromString(userDto.id)
                    userPermissionsRepository?.getEnabledFeatureKeys(userId) ?: emptyList()
                }
                else -> {
                    // STUDENT or other roles don't get features in login response
                    null
                }
            }
        } catch (e: Exception) {
            println("Warning: Failed to fetch enabled features for user ${userDto.id}: ${e.message}")
            return null
        }
    }

    suspend fun authenticateUserWithFCM(
        request: UserLoginFCMRequest,
    ): UserLoginResponse {
        // First authenticate user normally
        val loginResponse = authenticateUser(UserLoginRequest(request.mobileNumber, request.password))

        // If authentication successful and FCM data provided, update token
        if (request.fcmToken != null && request.platform != null && fcmService != null) {
            try {
                updateFCMTokenOnLogin(UUID.fromString(loginResponse.user[0].id), request.fcmToken, request.deviceId, request.platform)
            } catch (e: Exception) {
                // Log FCM update failure but don't fail the login
                println("Warning: Failed to update FCM token for user ${loginResponse.user[0].id}: ${e.message}")
            }
        }

        return loginResponse
    }

    /**
     * Update FCM token on user login
     * @param userId User ID as UUID
     * @param fcmToken FCM token to be saved
     * @param deviceId Optional device identifier
     * @param platform Platform (e.g., "android", "ios")
     * @return Boolean indicating success
     */
    suspend fun updateFCMTokenOnLogin(
        userId: UUID,
        fcmToken: String,
        deviceId: String?,
        platform: String
    ): Boolean {

        val tenant = TenantContextHolder.getTenant()
            ?: throw ApiException("Tenant context missing in service", HttpStatusCode.InternalServerError)

        println("[FCM] Login token update started | userId=$userId")

        // Step 1: Validate FCM token
        if (fcmToken.isBlank()) {
            println("[FCM] Validation failed: FCM token is empty | userId=$userId")
            throw ApiException("FCM token cannot be empty", HttpStatusCode.BadRequest)
        }

        // Step 2: Validate platform
        if (platform.isBlank()) {
            println("[FCM] Validation failed: Platform is empty | userId=$userId")
            throw ApiException("Platform cannot be empty", HttpStatusCode.BadRequest)
        }

        println("[FCM] Validation passed | platform=$platform | deviceId=$deviceId")

        // Step 3: Check user existence
        val userExists = try {
            println("[FCM] Checking if user exists | userId=$userId")
            getUserById(userId)
            true
        } catch (e: ApiException) {
            println("[FCM] User not found | userId=$userId | error=${e.message}")
            false
        }

        if (!userExists) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }

        println("[FCM] User exists | userId=$userId")

        // Step 4: Check FCM service availability
        if (fcmService == null) {
            println("[FCM] Warning: FCM service not available | userId=$userId")
            return false
        }

        // Step 5: Save FCM token
        val tokenRequest = FCMTokenRequest(
            token = fcmToken,
            deviceId = deviceId,
            platform = platform
        )

        println("[FCM] Saving FCM token | userId=$userId | platform=$platform")

        val result = fcmService.saveToken(userId, tokenRequest)

        println("[FCM] Token save result=$result | userId=$userId")

        return result
    }

    /**
     * Send password reset OTP to user's email
     * User provides mobile number, system finds their email and sends OTP
     */
    suspend fun sendPasswordResetOtp(mobileNumber: String): com.example.models.dto.ForgotPasswordSendOtpResponse {
        // Validate mobile number
        if (mobileNumber.isBlank()) {
            throw ApiException("Mobile number cannot be empty", HttpStatusCode.BadRequest)
        }

        // Find user by mobile number
        val users = userRepository.findByMobile(mobileNumber)
        if (users.isNullOrEmpty()) {
            throw ApiException("No account found with this mobile number", HttpStatusCode.NotFound)
        }

        val user = users[0]
        val email = user.email

        // Check if required services are available
        if (otpRepository == null || emailService == null) {
            throw ApiException("Password reset service not available", HttpStatusCode.ServiceUnavailable)
        }

        // Generate 6-digit OTP
        val otpCode = (100000..999999).random().toString()
        val expiresAt = java.time.LocalDateTime.now().plusMinutes(10)

        // Save OTP to database (reuse existing OTP table)
        otpRepository.createOtp(email, otpCode, expiresAt)

        // Send password reset email
        val emailSent = emailService.sendPasswordResetOtpEmail(email, otpCode)
        if (!emailSent) {
            throw ApiException("Failed to send password reset email", HttpStatusCode.InternalServerError)
        }

        return com.example.models.dto.ForgotPasswordSendOtpResponse(
            message = "Password reset code sent to your email",
            mobileNumber = mobileNumber,
            email = maskEmail(email)
        )
    }

    /**
     * Reset password using OTP verification
     */
    suspend fun resetPasswordWithOtp(
        mobileNumber: String,
        otpCode: String,
        newPassword: String
    ): UserDto {
        // Validate inputs
        if (mobileNumber.isBlank()) {
            throw ApiException("Mobile number cannot be empty", HttpStatusCode.BadRequest)
        }
        if (otpCode.isBlank()) {
            throw ApiException("OTP code cannot be empty", HttpStatusCode.BadRequest)
        }
        if (otpCode.length != 6) {
            throw ApiException("OTP code must be 6 digits", HttpStatusCode.BadRequest)
        }
        if (newPassword.isBlank()) {
            throw ApiException("New password cannot be empty", HttpStatusCode.BadRequest)
        }
        if (newPassword.length < 6) {
            throw ApiException("New password must be at least 6 characters", HttpStatusCode.BadRequest)
        }

        // Find user by mobile number
        val users = userRepository.findByMobile(mobileNumber)
        if (users.isNullOrEmpty()) {
            throw ApiException("No account found with this mobile number", HttpStatusCode.NotFound)
        }

        val user = users[0]
        val email = user.email

        // Check if OTP repository is available
        if (otpRepository == null) {
            throw ApiException("Password reset service not available", HttpStatusCode.ServiceUnavailable)
        }

        // Check attempt count first
        val attemptCount = otpRepository.getAttemptCount(email, otpCode)
        if (attemptCount >= 3) {
            throw ApiException(
                "Too many failed attempts. Please request a new password reset code.",
                HttpStatusCode.TooManyRequests
            )
        }

        // Verify OTP
        val isValid = otpRepository.verifyOtp(email, otpCode)
        if (!isValid) {
            val newAttemptCount = attemptCount + 1
            val remainingAttempts = 3 - newAttemptCount

            if (remainingAttempts > 0) {
                throw ApiException(
                    "Invalid or expired OTP code. $remainingAttempts attempts remaining.",
                    HttpStatusCode.BadRequest
                )
            } else {
                throw ApiException(
                    "Invalid or expired OTP code. Maximum attempts reached. Please request a new code.",
                    HttpStatusCode.TooManyRequests
                )
            }
        }

        // Hash new password
        val newHashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt())

        // Update password
        val userId = UUID.fromString(user.id)
        val updated = userRepository.updatePassword(userId, newHashedPassword)
        if (!updated) {
            throw ApiException("Failed to update password", HttpStatusCode.InternalServerError)
        }

        return getUserById(userId)
    }

    private fun validateCreateUserRequest(request: CreateUserRequest) {
        when {
            request.email.isBlank() -> throw ApiException("Email cannot be empty", HttpStatusCode.BadRequest)
            !isValidEmail(request.email) -> throw ApiException("Invalid email format", HttpStatusCode.BadRequest)
            request.mobileNumber.isBlank() -> throw ApiException("Mobile number cannot be empty", HttpStatusCode.BadRequest)
            request.mobileNumber.length > 15 -> throw ApiException("Mobile number is too long (max 15 characters)", HttpStatusCode.BadRequest)
            request.password.isBlank() -> throw ApiException("Password cannot be empty", HttpStatusCode.BadRequest)
            request.password.length < 6 -> throw ApiException("Password must be at least 6 characters", HttpStatusCode.BadRequest)
            request.firstName.isBlank() -> throw ApiException("First name cannot be empty", HttpStatusCode.BadRequest)
            request.firstName.length > 50 -> throw ApiException("First name is too long (max 50 characters)", HttpStatusCode.BadRequest)
            request.lastName.isBlank() -> throw ApiException("Last name cannot be empty", HttpStatusCode.BadRequest)
            request.lastName.length > 50 -> throw ApiException("Last name is too long (max 50 characters)", HttpStatusCode.BadRequest)
        }

        // Validate role
        try {
            UserRole.valueOf(request.role.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid role: ${request.role}", HttpStatusCode.BadRequest)
        }
    }

    private fun validateUpdateUserRequest(request: UpdateUserRequest) {
        when {
            request.email.isBlank() -> throw ApiException("Email cannot be empty", HttpStatusCode.BadRequest)
            !isValidEmail(request.email) -> throw ApiException("Invalid email format", HttpStatusCode.BadRequest)
            request.mobileNumber.isBlank() -> throw ApiException("Mobile number cannot be empty", HttpStatusCode.BadRequest)
            request.mobileNumber.length > 15 -> throw ApiException("Mobile number is too long (max 15 characters)", HttpStatusCode.BadRequest)
            request.firstName.isBlank() -> throw ApiException("First name cannot be empty", HttpStatusCode.BadRequest)
            request.firstName.length > 50 -> throw ApiException("First name is too long (max 50 characters)", HttpStatusCode.BadRequest)
            request.lastName.isBlank() -> throw ApiException("Last name cannot be empty", HttpStatusCode.BadRequest)
            request.lastName.length > 50 -> throw ApiException("Last name is too long (max 50 characters)", HttpStatusCode.BadRequest)
        }

        // Validate role
        try {
            UserRole.valueOf(request.role.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid role: ${request.role}", HttpStatusCode.BadRequest)
        }
    }

    private fun validateChangePasswordRequest(request: ChangePasswordRequest) {
        when {
            request.currentPassword.isBlank() -> throw ApiException("Current password cannot be empty", HttpStatusCode.BadRequest)
            request.newPassword.isBlank() -> throw ApiException("New password cannot be empty", HttpStatusCode.BadRequest)
            request.newPassword.length < 6 -> throw ApiException("New password must be at least 6 characters", HttpStatusCode.BadRequest)
            request.currentPassword == request.newPassword -> throw ApiException("New password must be different from current password", HttpStatusCode.BadRequest)
        }
    }

    private fun validateLoginRequest(request: UserLoginRequest) {
        when {
            request.mobileNumber.isBlank() -> throw ApiException("Email cannot be empty", HttpStatusCode.BadRequest)
            request.password.isBlank() -> throw ApiException("Password cannot be empty", HttpStatusCode.BadRequest)
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }

    /**
     * Mask email for privacy (e.g., "test@example.com" -> "t***@example.com")
     */
    private fun maskEmail(email: String): String {
        val parts = email.split("@")
        if (parts.size != 2) return email

        val username = parts[0]
        val domain = parts[1]

        return when {
            username.length <= 1 -> "${username}***@${domain}"
            username.length <= 3 -> "${username.first()}***@${domain}"
            else -> "${username.first()}***${username.last()}@${domain}"
        }
    }
}