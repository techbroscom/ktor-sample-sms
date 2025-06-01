package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import java.util.*

object AcademicYears : Table("academic_years") {
    val id = uuid("id").clientDefault { UUID.randomUUID() }
    val year = varchar("year", 9) // e.g., "2024-2025"
    val startDate = date("start_date")
    val endDate = date("end_date")
    val isActive = bool("is_active").default(false)

    override val primaryKey = PrimaryKey(id)
}