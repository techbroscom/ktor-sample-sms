package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserPermissions : Table("user_permissions") {
    val id = integer("id").autoIncrement()
    val userId = uuid("user_id") // From tenant's Users table
    val tenantId = varchar("tenant_id", 50).references(TenantConfig.tenantId)
    val featureId = integer("feature_id").references(Features.id)
    val isEnabled = bool("is_enabled").default(true)
    val grantedAt = datetime("granted_at").default(LocalDateTime.now())
    val grantedBy = uuid("granted_by").nullable() // Admin who granted permission
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId, tenantId, featureId)
    }
}
