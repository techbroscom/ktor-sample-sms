package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class ChatRoomMemberRepository {

    suspend fun addMember(roomId: UUID, userId: UUID, role: ChatMemberRole): UUID = tenantDbQuery {
        val memberId = UUID.randomUUID()
        ChatRoomMembers.insert {
            it[id] = memberId
            it[ChatRoomMembers.roomId] = roomId
            it[ChatRoomMembers.userId] = userId
            it[ChatRoomMembers.role] = role
            it[joinedAt] = LocalDateTime.now()
        }
        memberId
    }

    suspend fun bulkAddMembers(roomId: UUID, userIds: List<UUID>, role: ChatMemberRole): List<UUID> = tenantDbQuery {
        val memberIds = mutableListOf<UUID>()
        ChatRoomMembers.batchInsert(userIds) { userId ->
            val memberId = UUID.randomUUID()
            memberIds.add(memberId)
            this[ChatRoomMembers.id] = memberId
            this[ChatRoomMembers.roomId] = roomId
            this[ChatRoomMembers.userId] = userId
            this[ChatRoomMembers.role] = role
            this[ChatRoomMembers.joinedAt] = LocalDateTime.now()
        }
        memberIds
    }

    suspend fun findByRoomId(roomId: UUID): List<ChatRoomMemberDto> = tenantDbQuery {
        ChatRoomMembers
            .join(Users, JoinType.INNER, ChatRoomMembers.userId, Users.id)
            .selectAll()
            .where { (ChatRoomMembers.roomId eq roomId) and (ChatRoomMembers.isActive eq true) }
            .map { mapRowToDto(it) }
    }

    suspend fun findByUserId(userId: UUID): List<ChatRoomMemberDto> = tenantDbQuery {
        ChatRoomMembers
            .join(Users, JoinType.INNER, ChatRoomMembers.userId, Users.id)
            .selectAll()
            .where { (ChatRoomMembers.userId eq userId) and (ChatRoomMembers.isActive eq true) }
            .map { mapRowToDto(it) }
    }

    suspend fun updateRole(roomId: UUID, userId: UUID, role: ChatMemberRole): Boolean = tenantDbQuery {
        ChatRoomMembers.update({
            (ChatRoomMembers.roomId eq roomId) and (ChatRoomMembers.userId eq userId)
        }) {
            it[ChatRoomMembers.role] = role
        } > 0
    }

    suspend fun updateLastRead(roomId: UUID, userId: UUID): Boolean = tenantDbQuery {
        ChatRoomMembers.update({
            (ChatRoomMembers.roomId eq roomId) and (ChatRoomMembers.userId eq userId)
        }) {
            it[lastReadAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun removeMember(roomId: UUID, userId: UUID): Boolean = tenantDbQuery {
        ChatRoomMembers.update({
            (ChatRoomMembers.roomId eq roomId) and (ChatRoomMembers.userId eq userId)
        }) {
            it[isActive] = false
        } > 0
    }

    suspend fun isMember(roomId: UUID, userId: UUID): Boolean = tenantDbQuery {
        ChatRoomMembers.selectAll()
            .where {
                (ChatRoomMembers.roomId eq roomId) and
                        (ChatRoomMembers.userId eq userId) and
                        (ChatRoomMembers.isActive eq true)
            }
            .count() > 0
    }

    suspend fun getMemberRole(roomId: UUID, userId: UUID): ChatMemberRole? = tenantDbQuery {
        ChatRoomMembers.selectAll()
            .where {
                (ChatRoomMembers.roomId eq roomId) and
                        (ChatRoomMembers.userId eq userId) and
                        (ChatRoomMembers.isActive eq true)
            }
            .map { it[ChatRoomMembers.role] }
            .singleOrNull()
    }

    private fun mapRowToDto(row: ResultRow): ChatRoomMemberDto {
        return ChatRoomMemberDto(
            id = row[ChatRoomMembers.id].toString(),
            roomId = row[ChatRoomMembers.roomId].toString(),
            userId = row[ChatRoomMembers.userId].toString(),
            userName = "${row[Users.firstName]} ${row[Users.lastName]}",
            userRole = row[Users.role].name,
            chatRole = row[ChatRoomMembers.role].name,
            joinedAt = row[ChatRoomMembers.joinedAt].toString(),
            lastReadAt = row[ChatRoomMembers.lastReadAt]?.toString(),
            isActive = row[ChatRoomMembers.isActive]
        )
    }
}