package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Tenants : Table("tenants") {
    val id = uuid("id")
    val name = varchar("name", 100)
    val schema_name = varchar("schema_name", 50).uniqueIndex()
    val tenantNumber = integer("tenant_number").autoIncrement().uniqueIndex() // <- NEW
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}