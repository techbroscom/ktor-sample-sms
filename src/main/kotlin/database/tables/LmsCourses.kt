package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class CourseStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}

object LmsCourses : Table("lms_courses") {
    val id = uuid("id")
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val instructor = varchar("instructor", 255)
    val thumbnail = varchar("thumbnail", 500).nullable()
    val category = varchar("category", 100)
    val totalDuration = varchar("total_duration", 100).nullable() // e.g., "2 months"
    val status = enumerationByName("status", 20, CourseStatus::class).default(CourseStatus.DRAFT)
    val createdBy = uuid("created_by").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, category)
        index(false, status)
        index(false, title)
    }
}
