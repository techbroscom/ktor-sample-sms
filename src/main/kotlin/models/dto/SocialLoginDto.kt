package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SocialLoginRequest(
    val idToken: String,
    val provider: String, // "google" or "apple"
    val fcmToken: String? = null,
    val deviceId: String? = null,
    val platform: String? = null,
    val appleRealEmail: String? = null // Real email from Apple credential (first sign-in only)
)

@Serializable
data class LinkAppleRelayRequest(
    val relayEmail: String,       // The Apple private relay email
    val actualEmail: String,      // User's real email in the system
    val fcmToken: String? = null,
    val deviceId: String? = null,
    val platform: String? = null
)
