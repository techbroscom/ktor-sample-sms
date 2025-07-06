package com.example.services

import com.example.repositories.FCMTokenRepository
import com.example.models.*
import com.example.config.FCMConfig
import com.google.firebase.messaging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class FCMService(
    private val fcmTokenRepository: FCMTokenRepository
) {

    private val messaging = FCMConfig.getMessaging()

    suspend fun saveToken(userId: UUID, tokenRequest: FCMTokenRequest): Boolean {
        return withContext(Dispatchers.IO) {
            fcmTokenRepository.saveToken(
                userId = userId,
                token = tokenRequest.token,
                deviceId = tokenRequest.deviceId,
                platform = tokenRequest.platform
            )
        }
    }

    suspend fun sendPersonalNotification(request: PersonalNotificationRequest): NotificationResponse {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = fcmTokenRepository.getTokensByUserId(UUID.fromString(request.userId))

                if (tokens.isEmpty()) {
                    return@withContext NotificationResponse(
                        success = false,
                        message = "No FCM tokens found for user ${request.userId}"
                    )
                }

                val message = MulticastMessage.builder()
                    .setNotification(
                        Notification.builder()
                            .setTitle(request.title)
                            .setBody(request.body)
                            .build()
                    )
                    .putAllData(request.data)
                    .addAllTokens(tokens)
                    .setAndroidConfig(
                        AndroidConfig.builder()
                            .setNotification(
                                AndroidNotification.builder()
                                    .setIcon("ic_notification")
                                    .setColor("#FF0000")
                                    .setPriority(AndroidNotification.Priority.HIGH)
                                    .build()
                            )
                            .build()
                    )
                    .setApnsConfig(
                        ApnsConfig.builder()
                            .setAps(
                                Aps.builder()
                                    .setAlert(
                                        ApsAlert.builder()
                                            .setTitle(request.title)
                                            .setBody(request.body)
                                            .build()
                                    )
                                    .setBadge(1)
                                    .setSound("default")
                                    .build()
                            )
                            .build()
                    )
                    .build()

                val response = messaging.sendEachForMulticast(message)

                // Handle failed tokens
                handleFailedTokens(tokens, response)

                NotificationResponse(
                    success = true,
                    message = "Personal notification sent successfully",
                    sentCount = response.successCount,
                    failedCount = response.failureCount
                )

            } catch (e: Exception) {
                NotificationResponse(
                    success = false,
                    message = "Failed to send personal notification: ${e.message}"
                )
            }
        }
    }

    suspend fun sendBroadcastNotification(request: BroadcastNotificationRequest): NotificationResponse {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = if (request.targetRole != null) {
//                    fcmTokenRepository.getTokensByRole(request.schoolId, request.targetRole)
                    fcmTokenRepository.getTokensBySchool(request.schoolId)
                } else {
                    fcmTokenRepository.getTokensBySchool(request.schoolId)
                }

                if (tokens.isEmpty()) {
                    return@withContext NotificationResponse(
                        success = false,
                        message = "No FCM tokens found for school ${request.schoolId}"
                    )
                }

                // Send in batches (FCM allows max 500 tokens per request)
                val batchSize = 500
                var totalSent = 0
                var totalFailed = 0

                tokens.chunked(batchSize).forEach { batch ->
                    val message = MulticastMessage.builder()
                        .setNotification(
                            Notification.builder()
                                .setTitle(request.title)
                                .setBody(request.body)
                                .build()
                        )
                        .putAllData(request.data)
                        .addAllTokens(batch)
                        .setAndroidConfig(
                            AndroidConfig.builder()
                                .setNotification(
                                    AndroidNotification.builder()
                                        .setIcon("ic_notification")
                                        .setColor("#FF0000")
                                        .setPriority(AndroidNotification.Priority.HIGH)
                                        .build()
                                )
                                .build()
                        )
                        .setApnsConfig(
                            ApnsConfig.builder()
                                .setAps(
                                    Aps.builder()
                                        .setAlert(
                                            ApsAlert.builder()
                                                .setTitle(request.title)
                                                .setBody(request.body)
                                                .build()
                                        )
                                        .setBadge(1)
                                        .setSound("default")
                                        .build()
                                )
                                .build()
                        )
                        .build()

                    val response = messaging.sendEachForMulticast(message)

                    // Handle failed tokens
                    handleFailedTokens(batch, response)

                    totalSent += response.successCount
                    totalFailed += response.failureCount
                }

                NotificationResponse(
                    success = true,
                    message = "Broadcast notification sent successfully",
                    sentCount = totalSent,
                    failedCount = totalFailed
                )

            } catch (e: Exception) {
                NotificationResponse(
                    success = false,
                    message = "Failed to send broadcast notification: ${e.message}"
                )
            }
        }
    }

    private fun handleFailedTokens(tokens: List<String>, response: BatchResponse) {
        response.responses.forEachIndexed { index, sendResponse ->
            if (!sendResponse.isSuccessful) {
                val error = sendResponse.exception
                if (error is FirebaseMessagingException) {
                    // Handle invalid tokens using string comparison or MessagingErrorCode enum
                    when (error.messagingErrorCode) {
                        MessagingErrorCode.UNREGISTERED,
                        MessagingErrorCode.INVALID_ARGUMENT -> {
                            // Deactivate invalid token
                            fcmTokenRepository.deactivateToken(tokens[index])
                        }
                        else -> {
                            // Log other errors for debugging
                            println("FCM Error for token ${tokens[index]}: ${error.messagingErrorCode}")
                        }
                    }
                }
            }
        }
    }

    suspend fun subscribeToTopic(userId: UUID, topic: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = fcmTokenRepository.getTokensByUserId(userId)
                if (tokens.isNotEmpty()) {
                    messaging.subscribeToTopic(tokens, topic)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun unsubscribeFromTopic(userId: UUID, topic: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = fcmTokenRepository.getTokensByUserId(userId)
                if (tokens.isNotEmpty()) {
                    messaging.unsubscribeFromTopic(tokens, topic)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}