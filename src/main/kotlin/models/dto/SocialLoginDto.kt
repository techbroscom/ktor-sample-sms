package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginRequest(
    val idToken: String,
    val provider: String, // "google" or "apple"
    val fcmToken: String? = null,
    val deviceId: String? = null,
    val platform: String? = null
)
