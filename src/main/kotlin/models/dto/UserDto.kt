package com.example.models.dto

import com.example.database.tables.UserRole
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val mobileNumber: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val createdAt: String,
    val updatedAt: String?
)

@Serializable
data class CreateUserRequest(
    val email: String,
    val mobileNumber: String,
    val password: String,
    val role: String, // ADMIN, STAFF, STUDENT
    val firstName: String,
    val lastName: String
)

@Serializable
data class UpdateUserRequest(
    val email: String,
    val mobileNumber: String,
    val role: String, // ADMIN, STAFF, STUDENT
    val firstName: String,
    val lastName: String
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class UserLoginRequest(
    val mobileNumber: String,
    val password: String
)

@Serializable
data class UserLoginFCMRequest(
    val mobileNumber: String,
    val password: String,
    val fcmToken: String?,
    val deviceId: String?,
    val platform: String?
)

@Serializable
data class UserLoginResponse(
    val user: List<UserDto>,
    val token: String? = null // For future JWT implementation
)