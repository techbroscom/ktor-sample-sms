package com.example.repositories

import com.example.database.tables.FCMTokens
import com.example.database.tables.SchoolConfig
import com.example.database.tables.UserRole
import com.example.database.tables.Users
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.util.UUID

class FCMTokenRepository {

    suspend fun saveToken(
        userId: UUID,
        token: String,
        deviceId: String?,
        platform: String
    ): Boolean = tenantDbQuery {

        println("[FCM][DB] saveToken | userId=$userId | deviceId=$deviceId")

        val existingToken = FCMTokens
            .selectAll()
            .where {
                (FCMTokens.userId eq userId) and
                        (FCMTokens.deviceId eq deviceId)
            }
            .singleOrNull()

        if (existingToken != null) {
            FCMTokens.update({
                (FCMTokens.userId eq userId) and
                        (FCMTokens.deviceId eq deviceId)
            }) {
                it[FCMTokens.token] = token
                it[FCMTokens.platform] = platform
                it[isActive] = true
                it[updatedAt] = LocalDateTime.now()
            }
        } else {
            FCMTokens.insert {
                it[FCMTokens.userId] = userId
                it[this.token] = token
                it[this.deviceId] = deviceId
                it[this.platform] = platform
                it[isActive] = true
            }
        }

        true
    }

    suspend fun getTokensByUserId(userId: UUID): List<String> = tenantDbQuery {
        FCMTokens
            .selectAll()
            .where {
                (FCMTokens.userId eq userId) and
                        (FCMTokens.isActive eq true)
            }
            .map { it[FCMTokens.token] }
    }

    suspend fun getTokensBySchool(): List<String> = tenantDbQuery {
        (FCMTokens innerJoin Users)
            .selectAll()
            .where { FCMTokens.isActive eq true }
            .map { it[FCMTokens.token] }
    }

    suspend fun getTokensByRole(role: UserRole): List<String> = tenantDbQuery {
        (FCMTokens innerJoin Users)
            .selectAll()
            .where {
                (Users.role eq role) and
                        (FCMTokens.isActive eq true)
            }
            .map { it[FCMTokens.token] }
    }

    suspend fun deactivateToken(token: String): Boolean = tenantDbQuery {
        FCMTokens.update({ FCMTokens.token eq token }) {
            it[isActive] = false
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteExpiredTokens(days: Int = 30): Int = tenantDbQuery {
        val cutoffDate = LocalDateTime.now().minusDays(days.toLong())
        FCMTokens.deleteWhere {
            (updatedAt less cutoffDate) and
                    (isActive eq false)
        }
    }

    suspend fun getAllActiveTokens(): List<String> = tenantDbQuery {
        FCMTokens
            .selectAll()
            .where { FCMTokens.isActive eq true }
            .map { it[FCMTokens.token] }
    }

    suspend fun getTokensByPlatform(platform: String): List<String> = tenantDbQuery {
        FCMTokens
            .selectAll()
            .where {
                (FCMTokens.platform eq platform) and
                        (FCMTokens.isActive eq true)
            }
            .map { it[FCMTokens.token] }
    }

    suspend fun getTokenByDeviceId(deviceId: String): String? = tenantDbQuery {
        FCMTokens
            .selectAll()
            .where {
                (FCMTokens.deviceId eq deviceId) and
                        (FCMTokens.isActive eq true)
            }
            .singleOrNull()
            ?.get(FCMTokens.token)
    }
}
