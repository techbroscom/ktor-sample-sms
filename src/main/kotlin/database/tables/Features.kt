package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Features : Table("features") {
    val id = integer("id").autoIncrement()
    val featureKey = varchar("feature_key", 100).uniqueIndex()
    val name = varchar("name", 100)
    val description = text("description")
    val category = varchar("category", 50).nullable()
    val isActive = bool("is_active").default(true)
    val defaultEnabled = bool("default_enabled").default(false)

    // Feature Limits Configuration
    val hasLimit = bool("has_limit").default(false)
    val limitType = varchar("limit_type", 50).nullable()
    val limitValue = integer("limit_value").nullable()
    val limitUnit = varchar("limit_unit", 50).nullable()

    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
