package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object TransportStops : Table("transport_stops") {
    val id = uuid("id")
    val routeId = uuid("route_id") references TransportRoutes.id
    val name = varchar("name", 100)
    val orderIndex = integer("order_index")
    val monthlyFee = decimal("monthly_fee", 10, 2)
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(routeId, name)
    }
}