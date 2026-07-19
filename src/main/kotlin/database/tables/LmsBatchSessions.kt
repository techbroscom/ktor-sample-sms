package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.time
import java.time.LocalDateTime

enum class SessionStatus {
    UPCOMING,
    LIVE,
    COMPLETED,
    CANCELLED
}

object LmsBatchSessions : Table("lms_batch_sessions") {
    val id = uuid("id")
    val batchId = uuid("batch_id").references(LmsBatches.id)
    val sectionId = uuid("section_id").references(LmsSections.id)
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val scheduledDate = date("scheduled_date")
    val startTime = time("start_time")
    val endTime = time("end_time")
    val meetingLink = varchar("meeting_link", 500).nullable()
    val providerMeetingId = varchar("provider_meeting_id", 255).nullable() // Zoho webinarKey, Zoom meeting ID, etc.
    val status = enumerationByName("status", 20, SessionStatus::class).default(SessionStatus.UPCOMING)
    val order = integer("session_order")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, batchId)
        index(false, sectionId)
        index(false, scheduledDate)
        index(false, status)
    }
}
