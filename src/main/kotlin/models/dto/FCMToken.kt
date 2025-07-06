package com.example.models

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FCMTokenRequest(
    val token: String,
    val deviceId: String? = null,
    val platform: String = "android"
)

@Serializable
data class BroadcastNotificationRequest(
    val title: String,
    val body: String,
    val schoolId: Int,
    val targetRole: String? = null, // "student", "teacher", "admin" or null for all
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class PersonalNotificationRequest(
    val title: String,
    val body: String,
    val userId: String,
    val data: Map<String, String> = emptyMap()
)

@Serializable
data class NotificationResponse(
    val success: Boolean,
    val message: String,
    val sentCount: Int = 0,
    val failedCount: Int = 0
)