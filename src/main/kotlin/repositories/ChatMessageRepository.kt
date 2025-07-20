package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class ChatMessageRepository {

    suspend fun create(request: SendMessageRequest, senderId: UUID): UUID = tenantDbQuery {
        val messageId = UUID.randomUUID()
        ChatMessages.insert {
            it[id] = messageId
            it[roomId] = UUID.fromString(request.roomId)
            it[ChatMessages.senderId] = senderId
            it[messageType] = ChatMessageType.valueOf(request.messageType)
            it[content] = request.content
            it[fileUrl] = request.fileUrl
            it[fileName] = request.fileName
            it[replyToMessageId] = request.replyToMessageId?.let { UUID.fromString(it) }
            it[sentAt] = LocalDateTime.now()
        }
        messageId
    }

    suspend fun findById(id: UUID): ChatMessageDto? = tenantDbQuery {
        val replyMsgAlias = ChatMessages.alias("reply_msg")

        ChatMessages
            .join(Users, JoinType.INNER, ChatMessages.senderId, Users.id)
            .leftJoin(replyMsgAlias, { ChatMessages.replyToMessageId }, { replyMsgAlias[ChatMessages.id] })
            .selectAll()
            .where { ChatMessages.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByRoomId(roomId: UUID, limit: Int = 50, offset: Long = 0): List<ChatMessageDto> = tenantDbQuery {
        val replyMsg = ChatMessages.alias("reply_msg")

        ChatMessages
            .join(Users, JoinType.INNER, ChatMessages.senderId, Users.id)
            .leftJoin(replyMsg, { ChatMessages.replyToMessageId }, { replyMsg[ChatMessages.id] })
            .selectAll()
            .where { (ChatMessages.roomId eq roomId) and (ChatMessages.isDeleted eq false) }
            .orderBy(ChatMessages.sentAt to SortOrder.DESC)
            .limit(limit).offset(offset)
            .map { mapRowToDto(it) }
            .reversed() // Return in chronological order
    }

    suspend fun update(id: UUID, request: EditMessageRequest): Boolean = tenantDbQuery {
        ChatMessages.update({ ChatMessages.id eq id }) {
            it[content] = request.content
            it[isEdited] = true
            it[editedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: UUID): Boolean = tenantDbQuery {
        ChatMessages.update({ ChatMessages.id eq id }) {
            it[isDeleted] = true
        } > 0
    }

    suspend fun getUnreadCount(roomId: UUID, userId: UUID, lastReadAt: LocalDateTime?): Long = tenantDbQuery {
        val query = ChatMessages.selectAll()
            .where {
                (ChatMessages.roomId eq roomId) and
                        (ChatMessages.isDeleted eq false) and
                        (ChatMessages.senderId neq userId) // Don't count own messages
            }

        if (lastReadAt != null) {
            query.andWhere { ChatMessages.sentAt greater lastReadAt }
        }

        query.count()
    }

    private fun mapRowToDto(row: ResultRow): ChatMessageDto {
        val replyMsg = ChatMessages.alias("reply_msg")

        return ChatMessageDto(
            id = row[ChatMessages.id].toString(),
            roomId = row[ChatMessages.roomId].toString(),
            senderId = row[ChatMessages.senderId].toString(),
            senderName = "${row[Users.firstName]} ${row[Users.lastName]}",
            messageType = row[ChatMessages.messageType].name,
            content = row[ChatMessages.content],
            fileUrl = row[ChatMessages.fileUrl],
            fileName = row[ChatMessages.fileName],
            replyToMessageId = row[ChatMessages.replyToMessageId]?.toString(),
            replyToContent = row.getOrNull(replyMsg[ChatMessages.content]),
            isEdited = row[ChatMessages.isEdited],
            isDeleted = row[ChatMessages.isDeleted],
            sentAt = row[ChatMessages.sentAt].toString(),
            editedAt = row[ChatMessages.editedAt]?.toString()
        )
    }
}