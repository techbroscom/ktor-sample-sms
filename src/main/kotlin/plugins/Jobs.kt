package com.example.plugins

import com.example.jobs.FCMCleanupJob
import com.example.jobs.OtpCleanupJob
import com.example.repositories.FCMTokenRepository
import com.example.repositories.OtpRepository
import com.example.services.TenantService
import io.ktor.server.application.*

fun Application.configureJobs() {

    val otpRepository = OtpRepository()
    val tenantService = TenantService()
    val otpCleanupJob = OtpCleanupJob(otpRepository, tenantService, this)

    val fcmTokenRepository = FCMTokenRepository()
    val fcmCleanupJob = FCMCleanupJob(fcmTokenRepository, tenantService, this)

    otpCleanupJob.start()
    fcmCleanupJob.start()

    // Stop job on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        otpCleanupJob.stop()
        fcmCleanupJob.stop()
    }
}
