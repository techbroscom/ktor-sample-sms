package com.example.models.websocket

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketMessage(
    val type: String,
    val data: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class JoinRoomEvent(
    val roomId: String,
    val userId: String
)

@Serializable
data class LeaveRoomEvent(
    val roomId: String,
    val userId: String
)

@Serializable
data class NewMessageEvent(
    val messageId: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val messageType: String,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val replyToMessageId: String? = null,
    val sentAt: String
)

@Serializable
data class MessageEditedEvent(
    val messageId: String,
    val roomId: String,
    val content: String,
    val editedAt: String
)

@Serializable
data class MessageDeletedEvent(
    val messageId: String,
    val roomId: String
)

@Serializable
data class TypingEvent(
    val roomId: String,
    val userId: String,
    val userName: String,
    val isTyping: Boolean
)

@Serializable
data class UserOnlineEvent(
    val userId: String,
    val userName: String,
    val isOnline: Boolean,
    val lastSeen: String? = null
)

@Serializable
data class RoomMemberAddedEvent(
    val roomId: String,
    val userId: String,
    val userName: String,
    val addedBy: String
)

@Serializable
data class RoomMemberRemovedEvent(
    val roomId: String,
    val userId: String,
    val userName: String,
    val removedBy: String
)

@Serializable
data class MessageReadEvent(
    val roomId: String,
    val userId: String,
    val lastReadAt: String
)

@Serializable
data class ErrorEvent(
    val error: String,
    val code: String? = null
)