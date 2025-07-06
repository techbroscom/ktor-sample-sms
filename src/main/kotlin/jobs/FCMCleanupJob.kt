package com.example.jobs

import com.example.repositories.FCMTokenRepository
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours

class FCMCleanupJob(
    private val fcmTokenRepository: FCMTokenRepository,
    private val application: Application
) {
    private var job: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        job = coroutineScope.launch {
            while (isActive) {
                try {
                    // Clean up expired tokens every 24 hours
                    val deletedCount = fcmTokenRepository.deleteExpiredTokens()
                    application.log.info("FCM Cleanup: Deleted $deletedCount expired tokens")

                    delay(24.hours)
                } catch (e: Exception) {
                    application.log.error("FCM Cleanup Job failed: ${e.message}")
                    delay(1.hours) // Retry after 1 hour on error
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        coroutineScope.cancel()
    }
}