package com.example.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService {

    private val smtpUsername = "noreply@manisankarsms.co.in"
    private val smtpPassword = "9vinJfvTFFuP"

    private val properties = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", "smtp.zoho.in")
        put("mail.smtp.port", "587")
        put("mail.smtp.ssl.trust", "smtp.zoho.in")
    }

    suspend fun sendOtpEmail(
        recipientEmail: String,
        otpCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(smtpUsername, smtpPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(
                    InternetAddress(
                        smtpUsername,
                        "SchoolMate Security"
                    )
                )
                setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
                )
                subject = "Your SchoolMate OTP Code"

                val htmlContent = """
                    <html>
                        <body style="font-family: Arial, sans-serif;">
                            <div style="max-width:600px;margin:auto;padding:20px">
                                <h2>Your verification code</h2>
                                <div style="font-size:32px;font-weight:bold;
                                    letter-spacing:6px;margin:20px 0">
                                    $otpCode
                                </div>
                                <p>This OTP is valid for <b>10 minutes</b>.</p>
                                <p style="font-size:12px;color:#777">
                                    This is an automated message. Please do not reply.
                                </p>
                            </div>
                        </body>
                    </html>
                """.trimIndent()

                setContent(htmlContent, "text/html; charset=UTF-8")
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun sendPasswordResetOtpEmail(
        recipientEmail: String,
        otpCode: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val session = Session.getInstance(properties, object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(smtpUsername, smtpPassword)
                }
            })

            val message = MimeMessage(session).apply {
                setFrom(
                    InternetAddress(
                        smtpUsername,
                        "SchoolMate Security"
                    )
                )
                setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(recipientEmail)
                )
                subject = "Password Reset Code - SchoolMate"

                val htmlContent = """
                    <html>
                        <body style="font-family: Arial, sans-serif;">
                            <div style="max-width:600px;margin:auto;padding:20px;border:1px solid #e0e0e0;border-radius:8px">
                                <h2 style="color:#333">Password Reset Request</h2>
                                <p>You have requested to reset your password. Use the code below to reset your password:</p>
                                <div style="background:#f5f5f5;padding:20px;border-radius:8px;text-align:center;margin:20px 0">
                                    <div style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#2196F3">
                                        $otpCode
                                    </div>
                                </div>
                                <p><b>Important:</b> This code is valid for <b>10 minutes</b> only.</p>
                                <p style="color:#666">If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>
                                <hr style="border:none;border-top:1px solid #e0e0e0;margin:20px 0">
                                <p style="font-size:12px;color:#999">
                                    This is an automated message from SchoolMate. Please do not reply to this email.
                                </p>
                            </div>
                        </body>
                    </html>
                """.trimIndent()

                setContent(htmlContent, "text/html; charset=UTF-8")
            }

            Transport.send(message)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
