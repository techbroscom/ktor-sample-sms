package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object TenantConfig : Table("tenant_config") {
    val id = integer("id").autoIncrement()
    val tenantId = varchar("tenant_id", 50).uniqueIndex()
    val tenantSchemaName = varchar("schema_name", 50).uniqueIndex()
    val tenantName = varchar("tenant_name", 255)

    // Subscription/Billing
    val subscriptionStatus = varchar("subscription_status", 20) // TRIAL, ACTIVE, SUSPENDED, EXPIRED
    val isPaid = bool("is_paid").default(false)
    val subscriptionStartDate = datetime("subscription_start_date")
    val subscriptionEndDate = datetime("subscription_end_date").nullable()
    val planType = varchar("plan_type", 50).nullable()

    // Storage
    val storageAllocatedMB = long("storage_allocated_mb").default(5120) // Default 5GB
    val storageUsedMB = long("storage_used_mb").default(0)

    // Limits
    val maxUsers = integer("max_users").nullable()
    val maxStudents = integer("max_students").nullable()

    // Metadata
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
    val isActive = bool("is_active").default(true)
    val notes = text("notes").nullable()

    override val primaryKey = PrimaryKey(id)
}
