package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Self-reported attendance, recorded when a student successfully calls the
 * /join endpoint for a live session (i.e. they were enrolled and within the
 * join window). This proves the student requested the meeting link, not that
 * they stayed for the whole session - see joinCount/lastJoinedAt for repeated
 * joins within the same session.
 */
object LmsSessionAttendance : Table("lms_session_attendance") {
    val id = uuid("id")
    val sessionId = uuid("session_id").references(LmsBatchSessions.id)
    val userId = uuid("user_id").references(Users.id)
    val batchId = uuid("batch_id").references(LmsBatches.id) // denormalized for fast per-batch/user lookups
    val firstJoinedAt = datetime("first_joined_at").default(LocalDateTime.now())
    val lastJoinedAt = datetime("last_joined_at").default(LocalDateTime.now())
    val joinCount = integer("join_count").default(1)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(sessionId, userId)
        index(false, userId)
        index(false, batchId)
    }
}
