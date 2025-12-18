package com.example.plugins

import com.example.jobs.FCMCleanupJob
import com.example.jobs.OtpCleanupJob
import com.example.repositories.FCMTokenRepository
import com.example.repositories.OtpRepository
import com.example.repositories.UserRepository
import com.example.services.EmailService
import com.example.services.OtpService
import io.ktor.server.application.*

fun Application.configureJobs() {

    /*val userRepository = UserRepository()
    val otpRepository = OtpRepository()
    val emailService = EmailService()
    val otpService = OtpService(otpRepository, userRepository, emailService)
    val otpCleanupJob = OtpCleanupJob(otpService, this)

    val fcmTokenRepository = FCMTokenRepository()
    val fcmCleanupJob = FCMCleanupJob(fcmTokenRepository, this)

    otpCleanupJob.start()
    fcmCleanupJob.start()

    // Stop job on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        otpCleanupJob.stop()
        fcmCleanupJob.stop()
    }*/
}
