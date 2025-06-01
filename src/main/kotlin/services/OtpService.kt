package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.UserLoginResponse
import com.example.repositories.OtpRepository
import com.example.repositories.UserRepository
import io.ktor.http.*
import java.time.LocalDateTime
import kotlin.random.Random

class OtpService(
    private val otpRepository: OtpRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {

    init {
        println("OtpService initialized")
    }

    suspend fun sendOtp(email: String): String {
        println("Sending OTP to $email")
        // Verify user exists with this email
        val user = userRepository.findByEmail(email)
        if (user.isNullOrEmpty()) {
            throw ApiException("No account found with this email", HttpStatusCode.NotFound)
        } else
            println(user.toString())

        // Generate 6-digit OTP
        val otpCode = generateOtp()
        val expiresAt = LocalDateTime.now().plusMinutes(10)

        // Save OTP to database
        otpRepository.createOtp(email, otpCode, expiresAt)

        // Send email
        val emailSent = emailService.sendOtpEmail(email, otpCode)
        if (!emailSent) {
            throw ApiException("Failed to send OTP email", HttpStatusCode.InternalServerError)
        }

        return "OTP sent successfully to $email"
    }

    suspend fun verifyOtpAndLogin(email: String, otpCode: String): UserLoginResponse {
        // Check attempt count first
        val attemptCount = otpRepository.getAttemptCount(email, otpCode)
        if (attemptCount >= 3) {
            throw ApiException("Too many failed attempts. Please request a new OTP.", HttpStatusCode.TooManyRequests)
        }

        // Verify OTP
        val isValid = otpRepository.verifyOtp(email, otpCode)
        if (!isValid) {
            val newAttemptCount = attemptCount + 1
            val remainingAttempts = 3 - newAttemptCount

            if (remainingAttempts > 0) {
                throw ApiException("Invalid or expired OTP. $remainingAttempts attempts remaining.", HttpStatusCode.BadRequest)
            } else {
                throw ApiException("Invalid or expired OTP. Maximum attempts reached. Please request a new OTP.", HttpStatusCode.TooManyRequests)
            }
        }

        // Get user details
        val users = userRepository.findByEmail(email)
        if (users.isNullOrEmpty()) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }

        return UserLoginResponse(
            user = users,
            token = null // Implement JWT token generation here if needed
        )
    }

    private fun generateOtp(): String {
        return (100000..999999).random().toString()
    }

    suspend fun cleanupExpiredOtps() {
        otpRepository.cleanupExpiredOtps()
    }
}