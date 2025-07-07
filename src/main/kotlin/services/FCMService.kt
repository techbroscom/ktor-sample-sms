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
        println("=== FCM Broadcast Notification Debug ===")
        println("Request details:")
        println("  - Title: ${request.title}")
        println("  - Body: ${request.body}")
        println("  - School ID: ${request.schoolId}")
        println("  - Target Role: ${request.targetRole}")
        println("  - Data: ${request.data}")
        println("  - Timestamp: ${System.currentTimeMillis()}")

        return withContext(Dispatchers.IO) {
            try {
                println("Starting token retrieval...")
                val tokens = if (request.targetRole != null) {
                    println("Fetching tokens by role: ${request.targetRole}")
                    val roleTokens = fcmTokenRepository.getTokensByRole(request.targetRole)
                    println("Tokens found for role '${request.targetRole}': ${roleTokens.size}")
                    roleTokens
                } else {
                    println("Fetching tokens by school: ${request.schoolId}")
                    val schoolTokens = fcmTokenRepository.getTokensBySchool()
                    println("Tokens found for school: ${schoolTokens.size}")
                    schoolTokens
                }

                println("Token retrieval completed. Total tokens: ${tokens.size}")

                if (tokens.isEmpty()) {
                    println("ERROR: No FCM tokens found!")
                    return@withContext NotificationResponse(
                        success = false,
                        message = "No FCM tokens found for school ${request.schoolId}"
                    )
                }

                // Log first few tokens (masked for security)
                println("Sample tokens (first 3, masked):")
                tokens.take(3).forEachIndexed { index, token ->
                    println("  Token ${index + 1}: ${token.take(20)}...${token.takeLast(10)}")
                }

                // Check Firebase messaging instance
                println("Checking Firebase messaging instance...")
                println("Firebase messaging instance: ${messaging::class.java.name}")

                // Send in batches (FCM allows max 500 tokens per request)
                val batchSize = 500
                var totalSent = 0
                var totalFailed = 0
                val batches = tokens.chunked(batchSize)

                println("Processing ${batches.size} batches of tokens...")

                batches.forEachIndexed { batchIndex, batch ->
                    println("--- Processing batch ${batchIndex + 1}/${batches.size} ---")
                    println("Batch size: ${batch.size}")

                    try {
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

                        println("Message built successfully for batch ${batchIndex + 1}")
                        println("Sending multicast message...")

                        val startTime = System.currentTimeMillis()
                        val response = messaging.sendEachForMulticast(message)
                        val endTime = System.currentTimeMillis()

                        println("FCM Response received in ${endTime - startTime}ms")
                        println("Batch ${batchIndex + 1} results:")
                        println("  - Success count: ${response.successCount}")
                        println("  - Failure count: ${response.failureCount}")

                        // Log individual response details
                        if (response.failureCount > 0) {
                            println("  - Failed message details:")
                            response.responses.forEachIndexed { index, sendResponse ->
                                if (!sendResponse.isSuccessful) {
                                    println("    Token ${index + 1}: ${sendResponse.exception?.message}")
                                    sendResponse.exception?.let { exception ->
                                        println("    Exception type: ${exception::class.java.simpleName}")
                                        println("    Exception cause: ${exception.cause?.message}")
                                    }
                                }
                            }
                        }

                        // Handle failed tokens
                        println("Handling failed tokens for batch ${batchIndex + 1}...")
                        handleFailedTokens(batch, response)

                        totalSent += response.successCount
                        totalFailed += response.failureCount

                        println("Batch ${batchIndex + 1} completed. Running totals - Sent: $totalSent, Failed: $totalFailed")

                    } catch (batchException: Exception) {
                        println("ERROR in batch ${batchIndex + 1}: ${batchException.message}")
                        println("Batch exception type: ${batchException::class.java.simpleName}")
                        batchException.printStackTrace()
                        totalFailed += batch.size
                    }
                }

                println("=== Final Results ===")
                println("Total tokens processed: ${tokens.size}")
                println("Total sent successfully: $totalSent")
                println("Total failed: $totalFailed")
                println("Success rate: ${if (tokens.size > 0) (totalSent.toDouble() / tokens.size * 100).toInt() else 0}%")

                NotificationResponse(
                    success = true,
                    message = "Broadcast notification sent successfully",
                    sentCount = totalSent,
                    failedCount = totalFailed
                )

            } catch (e: Exception) {
                println("CRITICAL ERROR in sendBroadcastNotification:")
                println("Error message: ${e.message}")
                println("Error type: ${e::class.java.simpleName}")
                println("Error cause: ${e.cause?.message}")
                e.printStackTrace()

                // Check if it's a Firebase initialization issue
                if (e.message?.contains("Firebase") == true || e.message?.contains("authentication") == true) {
                    println("This appears to be a Firebase configuration issue!")
                    println("Check your Firebase service account key and credentials.")
                }

                NotificationResponse(
                    success = false,
                    message = "Failed to send broadcast notification: ${e.message}"
                )
            }
        }
    }

    // Enhanced handleFailedTokens function
    private fun handleFailedTokens(tokens: List<String>, response: BatchResponse) {
        println("=== Handling Failed Tokens ===")
        println("Total responses: ${response.responses.size}")

        val failedTokens = mutableListOf<String>()

        response.responses.forEachIndexed { index, sendResponse ->
            if (!sendResponse.isSuccessful) {
                val token = tokens[index]
                val exception = sendResponse.exception

                println("Failed token ${index + 1}:")
                println("  Token (masked): ${token.take(20)}...${token.takeLast(10)}")
                println("  Error: ${exception?.message}")

                when {
                    exception?.message?.contains("registration-token-not-registered") == true -> {
                        println("  Action: Token should be removed from database (unregistered)")
                        failedTokens.add(token)
                    }
                    exception?.message?.contains("invalid-registration-token") == true -> {
                        println("  Action: Token should be removed from database (invalid format)")
                        failedTokens.add(token)
                    }
                    exception?.message?.contains("authentication") == true -> {
                        println("  Action: Check Firebase authentication configuration")
                    }
                    exception?.message?.contains("quota") == true -> {
                        println("  Action: FCM quota exceeded, consider rate limiting")
                    }
                    else -> {
                        println("  Action: Investigate unknown error")
                    }
                }
            }
        }

        if (failedTokens.isNotEmpty()) {
            println("Tokens to be cleaned up: ${failedTokens.size}")
            // Here you would typically call a function to remove invalid tokens
            // removeInvalidTokens(failedTokens)
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