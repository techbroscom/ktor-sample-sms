package com.example.jobs

import com.example.repositories.FCMTokenRepository
import com.example.services.TenantService
import com.example.tenant.TenantContextHolder
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours
import org.slf4j.LoggerFactory

class FCMCleanupJob(
    private val fcmTokenRepository: FCMTokenRepository,
    private val tenantService: TenantService,
    private val application: Application
) {
    private val logger = LoggerFactory.getLogger(FCMCleanupJob::class.java)
    private var job: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        job = coroutineScope.launch {
            while (isActive) {
                try {
                    cleanupExpiredTokensForAllTenants()
                    delay(24.hours)
                } catch (e: Exception) {
                    logger.error("FCM Cleanup Job failed: ${e.message}", e)
                    delay(1.hours) // Retry after 1 hour on error
                }
            }
        }
    }

    private suspend fun cleanupExpiredTokensForAllTenants() {
        val tenants = tenantService.getAllTenants()
        logger.info("Starting FCM token cleanup for ${tenants.size} tenants")

        var successCount = 0
        var failureCount = 0
        var totalDeletedTokens = 0

        for (tenant in tenants) {
            try {
                // Set tenant context for this cleanup operation
                TenantContextHolder.setTenant(tenant)

                // Clean up expired tokens (older than 30 days by default)
                val deletedCount = fcmTokenRepository.deleteExpiredTokens()
                logger.info("FCM Cleanup for tenant ${tenant.name} (${tenant.schemaName}): Deleted $deletedCount expired tokens")
                totalDeletedTokens += deletedCount
                successCount++
            } catch (e: Exception) {
                logger.error("Failed to cleanup FCM tokens for tenant ${tenant.name} (${tenant.schemaName})", e)
                failureCount++
            } finally {
                // Always clear tenant context
                TenantContextHolder.clear()
            }
        }

        logger.info("FCM Cleanup completed: $successCount succeeded, $failureCount failed, $totalDeletedTokens total tokens deleted")
    }

    fun stop() {
        job?.cancel()
        coroutineScope.cancel()
    }
}