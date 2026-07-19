package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * @deprecated Session templates have been removed to simplify the course hierarchy.
 * Courses now have a flat structure: Course → Sections → (Batch Sessions are scheduled directly).
 * This table is kept temporarily for migration purposes only.
 */
object LmsSessionTemplates : Table("lms_session_templates") {
    val id = uuid("id")
    val sectionId = uuid("section_id").references(LmsSections.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val order = integer("session_order")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, sectionId)
    }
}
