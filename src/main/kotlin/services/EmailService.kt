package com.example.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

class EmailService {

    private val gmailUsername = "smstechbros@gmail.com"
    private val gmailPassword = "tfkdmhulwsvxfqos"

    private val properties = Properties().apply {
        put("mail.smtp.auth", "true")
        put("mail.smtp.starttls.enable", "true")
        put("mail.smtp.host", "smtp.gmail.com")
        put("mail.smtp.port", "587")
        put("mail.smtp.ssl.trust", "smtp.gmail.com")
    }

    suspend fun sendOtpEmail(recipientEmail: String, otpCode: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(gmailUsername, gmailPassword)
                    }
                })

                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(gmailUsername))
                    setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail))
                    subject = "Your Login OTP Code"

                    val htmlContent = """
                        <html>
                            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                                    <h2 style="color: #2c3e50; text-align: center;">Your Login Verification Code</h2>
                                    
                                    <div style="background-color: #f8f9fa; border-radius: 8px; padding: 30px; text-align: center; margin: 20px 0;">
                                        <h1 style="color: #007bff; font-size: 36px; margin: 0; letter-spacing: 8px;">$otpCode</h1>
                                    </div>
                                    
                                    <p>Hello,</p>
                                    <p>Use the above 6-digit code to complete your login. This code will expire in <strong>10 minutes</strong>.</p>
                                    
                                    <div style="background-color: #fff3cd; border: 1px solid #ffeaa7; border-radius: 4px; padding: 15px; margin: 20px 0;">
                                        <p style="margin: 0;"><strong>Security Note:</strong> If you didn't request this code, please ignore this email. Never share this code with anyone.</p>
                                    </div>
                                    
                                    <p style="color: #666; font-size: 14px; margin-top: 30px;">
                                        This is an automated message, please do not reply to this email.
                                    </p>
                                </div>
                            </body>
                        </html>
                    """.trimIndent()

                    setContent(htmlContent, "text/html; charset=utf-8")
                }

                Transport.send(message)
                true
            } catch (e: Exception) {
                println("Failed to send email: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }
}