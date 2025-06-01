package com.example.services

import com.example.database.tables.UserRole
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.UserRepository
import io.ktor.http.*
import org.mindrot.jbcrypt.BCrypt
import java.util.*

class UserService(private val userRepository: UserRepository) {

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

    suspend fun authenticateUser(request: UserLoginRequest): UserLoginResponse {
        validateLoginRequest(request)

        val user = userRepository.findByEmail(request.mobileNumber)
            ?: throw ApiException("Invalid email or password", HttpStatusCode.Unauthorized)

        val passwordHash = userRepository.findPasswordHashByEmail(request.mobileNumber)
            ?: throw ApiException("Invalid email or password", HttpStatusCode.Unauthorized)

        if (!BCrypt.checkpw(request.password, passwordHash)) {
            throw ApiException("Invalid email or password", HttpStatusCode.Unauthorized)
        }

        return UserLoginResponse(
            user = user,
            token = null // Implement JWT token generation here if needed
        )
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
}