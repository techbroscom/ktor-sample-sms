package com.example.jobs

import com.example.services.OtpService
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours
import org.slf4j.LoggerFactory

class OtpCleanupJob(
    private val otpService: OtpService,
    private val scope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(OtpCleanupJob::class.java)
    private var cleanupJob: Job? = null

    fun start() {
        cleanupJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    otpService.cleanupExpiredOtps()
                    delay(1.hours)
                } catch (e: Exception) {
                    logger.error("Error during OTP cleanup", e)
                }
            }
        }
    }

    fun stop() {
        cleanupJob?.cancel()
    }
}