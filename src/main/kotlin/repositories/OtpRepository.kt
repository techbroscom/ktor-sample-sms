package com.example.repositories

import com.example.database.tables.OtpCodes
import com.example.utils.dbQuery
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import java.time.LocalDateTime
import java.util.*

class OtpRepository {

    suspend fun createOtp(email: String, otpCode: String, expiresAt: LocalDateTime): UUID = tenantDbQuery {
        // First, mark any existing unused OTPs for this email as used
        OtpCodes.update({ (OtpCodes.email eq email) and (OtpCodes.isUsed eq false) }) {
            it[isUsed] = true
        }

        // Create new OTP
        val otpId = UUID.randomUUID()
        OtpCodes.insert {
            it[id] = otpId
            it[OtpCodes.email] = email
            it[OtpCodes.otpCode] = otpCode
            it[OtpCodes.expiresAt] = expiresAt
            it[isUsed] = false
            it[attempts] = 0
            it[createdAt] = LocalDateTime.now()
        }
        otpId
    }

    suspend fun verifyOtp(email: String, otpCode: String): Boolean = tenantDbQuery {
        val now = LocalDateTime.now()

        // Find valid OTP
        val otpRecord = OtpCodes.selectAll()
            .where {
                (OtpCodes.email eq email) and
                        (OtpCodes.otpCode eq otpCode) and
                        (OtpCodes.isUsed eq false) and
                        (OtpCodes.expiresAt greater now) and
                        (OtpCodes.attempts less 3)
            }
            .singleOrNull()

        if (otpRecord != null) {
            // Mark OTP as used
            OtpCodes.update({ OtpCodes.id eq otpRecord[OtpCodes.id] }) {
                it[isUsed] = true
            }
            true
        } else {
            // Increment attempts for this email/code combination if exists
            OtpCodes.update({
                (OtpCodes.email eq email) and
                        (OtpCodes.otpCode eq otpCode) and
                        (OtpCodes.isUsed eq false)
            }) {
                it[attempts] = attempts + 1
            }
            false
        }
    }

    suspend fun cleanupExpiredOtps() = tenantDbQuery {
        val now = LocalDateTime.now()
        OtpCodes.deleteWhere { expiresAt less now }
    }

    suspend fun getAttemptCount(email: String, otpCode: String): Int = tenantDbQuery {
        OtpCodes.selectAll()
            .where {
                (OtpCodes.email eq email) and
                        (OtpCodes.otpCode eq otpCode) and
                        (OtpCodes.isUsed eq false)
            }
            .map { it[OtpCodes.attempts] }
            .maxOrNull() ?: 0
    }
}