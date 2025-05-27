package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Holidays : Table("holidays") {
    val id = integer("id").autoIncrement()
    val name = varchar("name", 255)
    val date = date("date")
    val description = varchar("description", 500).nullable()
    val isPublicHoliday = bool("is_public_holiday")

    override val primaryKey = PrimaryKey(id)
}