package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpRequest(
    val email: String
)

@Serializable
data class VerifyOtpRequest(
    val email: String,
    val otpCode: String
)

@Serializable
data class SendOtpResponse(
    val message: String,
    val email: String
)

// Password Reset DTOs
@Serializable
data class ForgotPasswordSendOtpRequest(
    val mobileNumber: String
)

@Serializable
data class ForgotPasswordSendOtpResponse(
    val message: String,
    val mobileNumber: String,
    val email: String // Masked email (e.g., "a***@example.com")
)

@Serializable
data class ForgotPasswordResetRequest(
    val mobileNumber: String,
    val otpCode: String,
    val newPassword: String
)