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
    val photoUrl: String? = null, // Deprecated, use imageUrl
    val imageUrl: String? = null, // S3 public URL
    val imageS3Key: String? = null, // S3 object key
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
    val lastName: String,
    val photoUrl: String? = null, // Deprecated, use imageUrl
    val imageUrl: String? = null, // S3 public URL (set via upload endpoint)
    val imageS3Key: String? = null // S3 object key (set via upload endpoint)
)

@Serializable
data class UpdateUserRequest(
    val email: String,
    val mobileNumber: String,
    val role: String, // ADMIN, STAFF, STUDENT
    val firstName: String,
    val lastName: String,
    val photoUrl: String? = null, // Deprecated, use imageUrl
    val imageUrl: String? = null, // S3 public URL (set via upload endpoint)
    val imageS3Key: String? = null // S3 object key (set via upload endpoint)
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
    val fcmToken: String? = null,
    val deviceId: String? = null,
    val platform: String? = null
)

@Serializable
data class UserLoginResponse(
    val user: List<UserDto>,
    val token: String? = null, // For future JWT implementation
    val enabledFeatures: List<String>? = null // Feature keys for ADMIN or assigned features for STAFF
)