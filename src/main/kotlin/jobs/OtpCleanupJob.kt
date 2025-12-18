package com.example.jobs

import com.example.repositories.OtpRepository
import com.example.services.TenantService
import com.example.tenant.TenantContextHolder
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.hours
import org.slf4j.LoggerFactory

class OtpCleanupJob(
    private val otpRepository: OtpRepository,
    private val tenantService: TenantService,
    private val scope: CoroutineScope
) {
    private val logger = LoggerFactory.getLogger(OtpCleanupJob::class.java)
    private var cleanupJob: Job? = null

    fun start() {
        cleanupJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    cleanupExpiredOtpsForAllTenants()
                    delay(1.hours)
                } catch (e: Exception) {
                    logger.error("Error during OTP cleanup", e)
                }
            }
        }
    }

    private suspend fun cleanupExpiredOtpsForAllTenants() {
        val tenants = tenantService.getAllTenants()
        logger.info("Starting OTP cleanup for ${tenants.size} tenants")

        var successCount = 0
        var failureCount = 0

        for (tenant in tenants) {
            try {
                // Set tenant context for this cleanup operation
                TenantContextHolder.setTenant(tenant)

                // Perform cleanup for this tenant
                val deletedCount = otpRepository.cleanupExpiredOtps()
                logger.info("OTP Cleanup for tenant ${tenant.name} (${tenant.schemaName}): Deleted $deletedCount expired OTPs")
                successCount++
            } catch (e: Exception) {
                logger.error("Failed to cleanup OTPs for tenant ${tenant.name} (${tenant.schemaName})", e)
                failureCount++
            } finally {
                // Always clear tenant context
                TenantContextHolder.clear()
            }
        }

        logger.info("OTP Cleanup completed: $successCount succeeded, $failureCount failed")
    }

    fun stop() {
        cleanupJob?.cancel()
    }
}