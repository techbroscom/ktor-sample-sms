package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ChatMessages : Table("chat_messages") {
    val id = uuid("id")
    val roomId = uuid("room_id").references(ChatRooms.id)
    val senderId = uuid("sender_id").references(Users.id)
    val messageType = enumerationByName("message_type", 20, ChatMessageType::class).default(ChatMessageType.TEXT)
    val content = text("content")
    val fileUrl = varchar("file_url", 500).nullable()
    val fileName = varchar("file_name", 255).nullable()
    val replyToMessageId = uuid("reply_to_message_id").references(ChatMessages.id).nullable()
    val isEdited = bool("is_edited").default(false)
    val isDeleted = bool("is_deleted").default(false)
    val sentAt = datetime("sent_at").default(LocalDateTime.now())
    val editedAt = datetime("edited_at").nullable()

    override val primaryKey = PrimaryKey(id)
}