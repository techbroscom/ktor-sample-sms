package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object OtpCodes : Table("otp_codes") {
    val id = uuid("id")
    val email = varchar("email", 255)
    val otpCode = varchar("otp_code", 6)
    val expiresAt = datetime("expires_at")
    val isUsed = bool("is_used").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val attempts = integer("attempts").default(0)

    override val primaryKey = PrimaryKey(id)
}