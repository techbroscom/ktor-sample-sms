package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object TransportRoutes : Table("transport_routes") {
    val id = uuid("id")
    val name = varchar("name", 100)
    val description = text("description").nullable()
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)
}
