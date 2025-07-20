package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class ChatRoomRepository {

    suspend fun create(request: CreateChatRoomRequest, createdBy: UUID): UUID = tenantDbQuery {
        val roomId = UUID.randomUUID()
        ChatRooms.insert {
            it[id] = roomId
            it[name] = request.name
            it[description] = request.description
            it[roomType] = ChatRoomType.valueOf(request.roomType)
            it[classId] = request.classId?.let { UUID.fromString(it) }
            it[classSubjectId] = request.classSubjectId?.let { UUID.fromString(it) }
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[ChatRooms.createdBy] = createdBy
            it[createdAt] = LocalDateTime.now()
        }
        roomId
    }

    suspend fun findById(id: UUID): ChatRoomDto? = tenantDbQuery {
        ChatRooms
            .join(Users, JoinType.INNER, ChatRooms.createdBy, Users.id)
            .join(AcademicYears, JoinType.INNER, ChatRooms.academicYearId, AcademicYears.id)
            .leftJoin(Classes)
            .leftJoin(ClassSubjects)
            .leftJoin(Subjects)
            .selectAll()
            .where { ChatRooms.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByClassId(classId: UUID, academicYearId: UUID): List<ChatRoomDto> = tenantDbQuery {
        ChatRooms
            .join(Users, JoinType.INNER, ChatRooms.createdBy, Users.id)
            .join(AcademicYears, JoinType.INNER, ChatRooms.academicYearId, AcademicYears.id)
            .leftJoin(Classes)
            .leftJoin(ClassSubjects)
            .leftJoin(Subjects)
            .selectAll()
            .where { (ChatRooms.classId eq classId) and (ChatRooms.academicYearId eq academicYearId) }
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassSubjectId(classSubjectId: UUID, academicYearId: UUID): ChatRoomDto? = tenantDbQuery {
        ChatRooms
            .join(Users, JoinType.INNER, ChatRooms.createdBy, Users.id)
            .join(AcademicYears, JoinType.INNER, ChatRooms.academicYearId, AcademicYears.id)
            .leftJoin(Classes)
            .leftJoin(ClassSubjects)
            .leftJoin(Subjects)
            .selectAll()
            .where { (ChatRooms.classSubjectId eq classSubjectId) and (ChatRooms.academicYearId eq academicYearId) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: UUID): List<ChatRoomDto> = tenantDbQuery {
        ChatRooms
            .join(Users, JoinType.INNER, ChatRooms.createdBy, Users.id)
            .join(AcademicYears, JoinType.INNER, ChatRooms.academicYearId, AcademicYears.id)
            .join(ChatRoomMembers, JoinType.INNER, ChatRooms.id, ChatRoomMembers.roomId)
            .leftJoin(Classes)
            .leftJoin(ClassSubjects)
            .leftJoin(Subjects)
            .selectAll()
            .where { (ChatRoomMembers.userId eq userId) and (ChatRoomMembers.isActive eq true) }
            .orderBy(ChatRooms.lastActivityAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: UUID, request: UpdateChatRoomRequest): Boolean = tenantDbQuery {
        ChatRooms.update({ ChatRooms.id eq id }) {
            it[name] = request.name
            it[description] = request.description
            it[isActive] = request.isActive
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun updateLastActivity(id: UUID): Boolean = tenantDbQuery {
        ChatRooms.update({ ChatRooms.id eq id }) {
            it[lastActivityAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: UUID): Boolean = tenantDbQuery {
        ChatRooms.deleteWhere { ChatRooms.id eq id } > 0
    }

    private fun mapRowToDto(row: ResultRow): ChatRoomDto {
        return ChatRoomDto(
            id = row[ChatRooms.id].toString(),
            name = row[ChatRooms.name],
            description = row[ChatRooms.description],
            roomType = row[ChatRooms.roomType].name,
            classId = row[ChatRooms.classId]?.toString(),
            className = row.getOrNull(Classes.className),
            classSubjectId = row[ChatRooms.classSubjectId]?.toString(),
            subjectName = row.getOrNull(Subjects.name),
            academicYearId = row[ChatRooms.academicYearId].toString(),
            academicYearName = row[AcademicYears.year],
            createdBy = row[ChatRooms.createdBy].toString(),
            createdByName = "${row[Users.firstName]} ${row[Users.lastName]}",
            isActive = row[ChatRooms.isActive],
            lastActivityAt = row[ChatRooms.lastActivityAt].toString(),
            memberCount = 0, // Will be populated separately
            createdAt = row[ChatRooms.createdAt].toString()
        )
    }
}