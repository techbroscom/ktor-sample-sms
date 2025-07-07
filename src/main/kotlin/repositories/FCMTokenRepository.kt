package com.example.repositories

import com.example.database.tables.FCMTokens
import com.example.database.tables.SchoolConfig
import com.example.database.tables.UserRole
import com.example.database.tables.Users
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class FCMTokenRepository {

    fun saveToken(userId: UUID, token: String, deviceId: String?, platform: String): Boolean {
        return transaction {
            // Check if token already exists for this user and device
            val existingToken = FCMTokens.selectAll().where {
                (FCMTokens.userId eq userId) and (FCMTokens.deviceId eq deviceId)
            }.singleOrNull()

            if (existingToken != null) {
                // Update existing token
                FCMTokens.update({
                    (FCMTokens.userId eq userId) and (FCMTokens.deviceId eq deviceId)
                }) {
                    it[FCMTokens.token] = token
                    it[FCMTokens.platform] = platform
                    it[FCMTokens.isActive] = true
                    it[FCMTokens.updatedAt] = LocalDateTime.now()
                }
            } else {
                // Insert new token
                FCMTokens.insert {
                    it[FCMTokens.userId] = userId
                    it[FCMTokens.token] = token
                    it[FCMTokens.deviceId] = deviceId
                    it[FCMTokens.platform] = platform
                    it[FCMTokens.isActive] = true
                }
            }
            true
        }
    }

    fun getTokensByUserId(userId: UUID): List<String> {
        return transaction {
            FCMTokens.selectAll().where {
                (FCMTokens.userId eq userId) and (FCMTokens.isActive eq true)
            }.map { it[FCMTokens.token] }
        }
    }

    fun getTokensBySchool(): List<String> {
        return transaction {
            (FCMTokens innerJoin Users).selectAll().where {
                (FCMTokens.isActive eq true)
            }.map { it[FCMTokens.token] }
        }
    }

    fun getTokensByRole(role: UserRole): List<String> {
        return transaction {
            (FCMTokens innerJoin Users).selectAll().where {
                        (Users.role eq role) and
                        (FCMTokens.isActive eq true)
            }.map { it[FCMTokens.token] }
        }
    }

    fun deactivateToken(token: String): Boolean {
        return transaction {
            FCMTokens.update({ FCMTokens.token eq token }) {
                it[FCMTokens.isActive] = false
                it[FCMTokens.updatedAt] = LocalDateTime.now()
            } > 0
        }
    }

    fun deleteExpiredTokens(days: Int = 30): Int {
        return transaction {
            val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
            FCMTokens.deleteWhere {
                (FCMTokens.updatedAt less cutoffDate) and (FCMTokens.isActive eq false)
            }
        }
    }

    fun getAllActiveTokens(): List<String> {
        return transaction {
            FCMTokens.selectAll().where {
                FCMTokens.isActive eq true
            }.map { it[FCMTokens.token] }
        }
    }

    fun getTokensByPlatform(platform: String): List<String> {
        return transaction {
            FCMTokens.selectAll().where {
                (FCMTokens.platform eq platform) and (FCMTokens.isActive eq true)
            }.map { it[FCMTokens.token] }
        }
    }

    fun getTokenByDeviceId(deviceId: String): String? {
        return transaction {
            FCMTokens.selectAll().where {
                (FCMTokens.deviceId eq deviceId) and (FCMTokens.isActive eq true)
            }.singleOrNull()?.get(FCMTokens.token)
        }
    }
}