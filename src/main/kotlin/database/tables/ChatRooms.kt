package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class ChatRoomType {
    CLASS, SUBJECT
}

enum class ChatMessageType {
    TEXT, IMAGE, FILE
}

enum class ChatMemberRole {
    ADMIN, MODERATOR, MEMBER
}

object ChatRooms : Table("chat_rooms") {
    val id = uuid("id")
    val name = varchar("name", 255)
    val description = text("description").nullable()
    val roomType = enumerationByName("room_type", 20, ChatRoomType::class)
    val classId = uuid("class_id").references(Classes.id).nullable()
    val classSubjectId = uuid("class_subject_id").references(ClassSubjects.id).nullable()
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)
    val createdBy = uuid("created_by").references(Users.id)
    val isActive = bool("is_active").default(true)
    val lastActivityAt = datetime("last_activity_at").default(LocalDateTime.now())
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(classId, academicYearId)
        uniqueIndex(classSubjectId, academicYearId)
    }
}