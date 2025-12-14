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
}
