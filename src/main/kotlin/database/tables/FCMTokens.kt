package com.example.database.tables

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.UUID

object FCMTokens : Table("fcm_tokens") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val userId = uuid("user_id").references(Users.id)
    val token = varchar("token", 255)
    val deviceId = varchar("device_id", 255).nullable()
    val platform = varchar("platform", 50) // e.g., "android", "ios"
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").clientDefault { LocalDateTime.now() }
    val updatedAt = datetime("updated_at").clientDefault { LocalDateTime.now() }

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(token) // Optional: ensures token is unique
    }
}