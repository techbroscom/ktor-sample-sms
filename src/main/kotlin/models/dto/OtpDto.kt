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