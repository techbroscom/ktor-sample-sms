package com.example.services

import com.example.repositories.FCMTokenRepository
import com.example.models.*
import com.example.config.FCMConfig
import com.example.tenant.TenantContextHolder
import com.google.firebase.messaging.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import java.util.UUID

class FCMService(
    private val fcmTokenRepository: FCMTokenRepository
) {

    private val messaging = FCMConfig.getMessaging()

    /* -------------------------------------------------------
     * SAVE TOKEN
     * ------------------------------------------------------- */
    suspend fun saveToken(userId: UUID, tokenRequest: FCMTokenRequest): Boolean {
        val tenant = TenantContextHolder.getTenant()
            ?: error("No tenant context found in saveToken")

        return withContext(Dispatchers.IO + TenantContextHolder.threadLocal.asContextElement(tenant)) {
            fcmTokenRepository.saveToken(
                userId = userId,
                token = tokenRequest.token,
                deviceId = tokenRequest.deviceId,
                platform = tokenRequest.platform
            )
        }
    }

    /* -------------------------------------------------------
     * PERSONAL NOTIFICATION
     * ------------------------------------------------------- */
    suspend fun sendPersonalNotification(
        request: PersonalNotificationRequest
    ): NotificationResponse {

        val tenant = TenantContextHolder.getTenant()
            ?: error("No tenant context found in sendPersonalNotification")

        return withContext(Dispatchers.IO + TenantContextHolder.threadLocal.asContextElement(tenant)) {
            try {
                val tokens = fcmTokenRepository.getTokensByUserId(UUID.fromString(request.userId))

                if (tokens.isEmpty()) {
                    return@withContext NotificationResponse(
                        success = false,
                        message = "No FCM tokens found for user ${request.userId}"
                    )
                }

                val message = buildMulticastMessage(
                    title = request.title,
                    body = request.body,
                    data = request.data,
                    tokens = tokens
                )

                val response = messaging.sendEachForMulticast(message)
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

    /* -------------------------------------------------------
     * BROADCAST NOTIFICATION
     * ------------------------------------------------------- */
    suspend fun sendBroadcastNotification(
        request: BroadcastNotificationRequest
    ): NotificationResponse {

        val tenant = TenantContextHolder.getTenant()
            ?: error("No tenant context found in sendBroadcastNotification")

        return withContext(Dispatchers.IO + TenantContextHolder.threadLocal.asContextElement(tenant)) {
            try {
                val tokens = if (request.targetRole != null) {
                    fcmTokenRepository.getTokensByRole(request.targetRole)
                } else {
                    fcmTokenRepository.getTokensBySchool()
                }

                if (tokens.isEmpty()) {
                    return@withContext NotificationResponse(
                        success = false,
                        message = "No FCM tokens found"
                    )
                }

                val batchSize = 500
                var totalSent = 0
                var totalFailed = 0

                tokens.chunked(batchSize).forEach { batch ->
                    val message = buildMulticastMessage(
                        title = request.title,
                        body = request.body,
                        data = request.data,
                        tokens = batch
                    )

                    val response = messaging.sendEachForMulticast(message)
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

    /* -------------------------------------------------------
     * MESSAGE BUILDER (ANDROID + IOS + WEB)
     * ------------------------------------------------------- */
    private fun buildMulticastMessage(
        title: String,
        body: String,
        data: Map<String, String>,
        tokens: List<String>
    ): MulticastMessage {

        return MulticastMessage.builder()
            .setNotification(
                Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build()
            )
            .putAllData(data)
            .addAllTokens(tokens)

            // ANDROID
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

            // IOS
            .setApnsConfig(
                ApnsConfig.builder()
                    .setAps(
                        Aps.builder()
                            .setAlert(
                                ApsAlert.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .build()
                            )
                            .setSound("default")
                            .build()
                    )
                    .build()
            )

            // 🌐 WEB (THIS FIXES YOUR ISSUE)
            .setWebpushConfig(
                WebpushConfig.builder()
                    .setNotification(
                        WebpushNotification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .setIcon("/favicon.png")
                            .setRequireInteraction(true)
                            .build()
                    )
                    .build()
            )

            .build()
    }

    /* -------------------------------------------------------
     * FAILED TOKEN HANDLING
     * ------------------------------------------------------- */
    private fun handleFailedTokens(tokens: List<String>, response: BatchResponse) {
        val invalidTokens = mutableListOf<String>()

        response.responses.forEachIndexed { index, sendResponse ->
            if (!sendResponse.isSuccessful) {
                val token = tokens[index]
                val message = sendResponse.exception?.message ?: ""

                if (
                    message.contains("registration-token-not-registered") ||
                    message.contains("invalid-registration-token")
                ) {
                    invalidTokens.add(token)
                }
            }
        }

        if (invalidTokens.isNotEmpty()) {
            // fcmTokenRepository.deleteTokens(invalidTokens)
            println("Invalid FCM tokens detected: ${invalidTokens.size}")
        }
    }

    /* -------------------------------------------------------
     * TOPIC MANAGEMENT
     * ------------------------------------------------------- */
    suspend fun subscribeToTopic(userId: UUID, topic: String): Boolean {
        val tenant = TenantContextHolder.getTenant()
            ?: error("No tenant context found in subscribeToTopic")

        return withContext(Dispatchers.IO + TenantContextHolder.threadLocal.asContextElement(tenant)) {
            try {
                val tokens = fcmTokenRepository.getTokensByUserId(userId)
                if (tokens.isNotEmpty()) {
                    messaging.subscribeToTopic(tokens, topic)
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }

    suspend fun unsubscribeFromTopic(userId: UUID, topic: String): Boolean {
        val tenant = TenantContextHolder.getTenant()
            ?: error("No tenant context found in unsubscribeFromTopic")

        return withContext(Dispatchers.IO + TenantContextHolder.threadLocal.asContextElement(tenant)) {
            try {
                val tokens = fcmTokenRepository.getTokensByUserId(userId)
                if (tokens.isNotEmpty()) {
                    messaging.unsubscribeFromTopic(tokens, topic)
                    true
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }
}
