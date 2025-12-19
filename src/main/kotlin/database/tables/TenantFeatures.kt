package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object TenantFeatures : Table("tenant_features") {
    val id = integer("id").autoIncrement()
    val tenantId = varchar("tenant_id", 50).references(TenantConfig.tenantId)
    val featureName = varchar("feature_name", 100)
    val isEnabled = bool("is_enabled").default(true)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(tenantId, featureName)
    }
}
