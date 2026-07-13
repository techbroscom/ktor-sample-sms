package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object LmsSections : Table("lms_sections") {
    val id = uuid("id")
    val courseId = uuid("course_id").references(LmsCourses.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val order = integer("section_order")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, courseId)
    }
}
