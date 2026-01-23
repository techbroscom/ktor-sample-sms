package com.example.plugins

import com.example.jobs.FCMCleanupJob
import com.example.jobs.OtpCleanupJob
import com.example.jobs.S3CleanupJob
import com.example.repositories.FCMTokenRepository
import com.example.repositories.OtpRepository
import com.example.repositories.FileRepository
import com.example.repositories.UserRepository
import com.example.repositories.SchoolConfigRepository
import com.example.repositories.PostImageRepository
import com.example.services.TenantService
import config.S3StorageConfig
import services.storage.S3CompatibleStorage
import io.ktor.server.application.*

fun Application.configureJobs() {

    val tenantService = TenantService()

    // OTP Cleanup Job
    val otpRepository = OtpRepository()
    val otpCleanupJob = OtpCleanupJob(otpRepository, tenantService, this)

    // FCM Cleanup Job
    val fcmTokenRepository = FCMTokenRepository()
    val fcmCleanupJob = FCMCleanupJob(fcmTokenRepository, tenantService, this)

    // S3 Cleanup Job
    val s3StorageConfig = S3StorageConfig.forBackblazeB2(
        accessKeyId = "005627b76e5aa4b0000000001",
        secretAccessKey = "K005COF4ZYJ1fXZnwgnuE/nsxyUwpBo",
        region = "us-east-005",
        bucketName = "schoolmate"
    )
    val s3Storage = S3CompatibleStorage(s3StorageConfig)
    val fileRepository = FileRepository()
    val userRepository = UserRepository()
    val schoolConfigRepository = SchoolConfigRepository()
    val postImageRepository = PostImageRepository(s3FileService = null)
    val s3CleanupJob = S3CleanupJob(
        fileRepository,
        userRepository,
        schoolConfigRepository,
        postImageRepository,
        tenantService,
        s3Storage,
        this
    )

    // Start all jobs
    otpCleanupJob.start()
    fcmCleanupJob.start()
    s3CleanupJob.start()

    // Stop jobs on shutdown
    environment.monitor.subscribe(ApplicationStopped) {
        otpCleanupJob.stop()
        fcmCleanupJob.stop()
        s3CleanupJob.stop()
    }
}
