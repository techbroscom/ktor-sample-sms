package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class BatchStatus {
    UPCOMING,
    ONGOING,
    COMPLETED,
    CANCELLED
}

object LmsBatches : Table("lms_batches") {
    val id = uuid("id")
    val courseId = uuid("course_id").references(LmsCourses.id)
    val name = varchar("name", 255) // e.g., "Batch 3 - Feb 2026"
    val startDate = date("start_date")
    val endDate = date("end_date")
    val price = decimal("price", 10, 2) // Full batch price
    val currency = varchar("currency", 10).default("INR")
    val maxSeats = integer("max_seats").nullable()
    val enrolledCount = integer("enrolled_count").default(0)
    val status = enumerationByName("status", 20, BatchStatus::class).default(BatchStatus.UPCOMING)
    val createdBy = uuid("created_by").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, courseId)
        index(false, status)
        index(false, startDate)
    }
}
