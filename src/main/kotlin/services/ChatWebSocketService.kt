package com.example.services

import com.example.models.websocket.*
import com.example.repositories.ChatRoomMemberRepository
import com.example.repositories.UserRepository
import com.example.websocket.ChatWebSocketManager
import java.util.*

class ChatWebSocketService(
    private val webSocketManager: ChatWebSocketManager,
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val userRepository: UserRepository
) {

    suspend fun handleJoinRoom(event: JoinRoomEvent, connectionId: String) {
        try {
            val userId = UUID.fromString(event.userId)
            val roomId = UUID.fromString(event.roomId)

            // Verify user is a member of the room
            if (!chatRoomMemberRepository.isMember(roomId, userId)) {
                webSocketManager.sendError(connectionId, "You are not a member of this room", "FORBIDDEN")
                return
            }

            // Get user details
            val user = userRepository.findById(userId)
            if (user == null) {
                webSocketManager.sendError(connectionId, "User not found", "USER_NOT_FOUND")
                return
            }

            val userName = "${user.firstName} ${user.lastName}"

            // Join the room
            webSocketManager.joinRoom(event.roomId, event.userId, userName)

            println("User $userName joined room ${event.roomId}")

        } catch (e: Exception) {
            webSocketManager.sendError(connectionId, "Failed to join room: ${e.message}", "JOIN_ROOM_ERROR")
        }
    }

    suspend fun handleLeaveRoom(event: LeaveRoomEvent, connectionId: String) {
        try {
            val userId = UUID.fromString(event.userId)

            // Get user details
            val user = userRepository.findById(userId)
            if (user == null) {
                webSocketManager.sendError(connectionId, "User not found", "USER_NOT_FOUND")
                return
            }

            val userName = "${user.firstName} ${user.lastName}"

            // Leave the room
            webSocketManager.leaveRoom(event.roomId, event.userId, userName)

            println("User $userName left room ${event.roomId}")

        } catch (e: Exception) {
            webSocketManager.sendError(connectionId, "Failed to leave room: ${e.message}", "LEAVE_ROOM_ERROR")
        }
    }

    suspend fun handleUserOnline(userId: String, connectionId: String) {
        try {
            val userUuid = UUID.fromString(userId)
            val user = userRepository.findById(userUuid)
            if (user == null) {
                webSocketManager.sendError(connectionId, "User not found", "USER_NOT_FOUND")
                return
            }

            val userName = "${user.firstName} ${user.lastName}"
            webSocketManager.handleUserOnline(userId, userName, connectionId)

            println("User $userName is online")

        } catch (e: Exception) {
            webSocketManager.sendError(connectionId, "Failed to set online status: ${e.message}", "ONLINE_STATUS_ERROR")
        }
    }

    suspend fun handleTyping(event: TypingEvent, connectionId: String) {
        try {
            val userId = UUID.fromString(event.userId)
            val roomId = UUID.fromString(event.roomId)

            // Verify user is a member of the room
            if (!chatRoomMemberRepository.isMember(roomId, userId)) {
                webSocketManager.sendError(connectionId, "You are not a member of this room", "FORBIDDEN")
                return
            }

            // Get user details
            val user = userRepository.findById(userId)
            if (user == null) {
                webSocketManager.sendError(connectionId, "User not found", "USER_NOT_FOUND")
                return
            }

            val userName = "${user.firstName} ${user.lastName}"

            // Handle typing
            webSocketManager.handleTyping(event.roomId, event.userId, userName, event.isTyping)

        } catch (e: Exception) {
            webSocketManager.sendError(connectionId, "Failed to handle typing: ${e.message}", "TYPING_ERROR")
        }
    }

    suspend fun broadcastNewMessage(messageDto: com.example.models.dto.ChatMessageDto) {
        val event = NewMessageEvent(
            messageId = messageDto.id,
            roomId = messageDto.roomId,
            senderId = messageDto.senderId,
            senderName = messageDto.senderName,
            content = messageDto.content,
            messageType = messageDto.messageType,
            fileUrl = messageDto.fileUrl,
            fileName = messageDto.fileName,
            replyToMessageId = messageDto.replyToMessageId,
            sentAt = messageDto.sentAt
        )

        webSocketManager.broadcastNewMessage(event)
    }

    suspend fun broadcastMessageEdited(messageDto: com.example.models.dto.ChatMessageDto) {
        val event = MessageEditedEvent(
            messageId = messageDto.id,
            roomId = messageDto.roomId,
            content = messageDto.content,
            editedAt = messageDto.editedAt ?: messageDto.sentAt
        )

        webSocketManager.broadcastMessageEdited(event)
    }

    suspend fun broadcastMessageDeleted(messageId: String, roomId: String) {
        val event = MessageDeletedEvent(
            messageId = messageId,
            roomId = roomId
        )

        webSocketManager.broadcastMessageDeleted(event)
    }

    suspend fun broadcastMessageRead(roomId: String, userId: String, lastReadAt: String) {
        val event = MessageReadEvent(
            roomId = roomId,
            userId = userId,
            lastReadAt = lastReadAt
        )

        webSocketManager.broadcastMessageRead(event)
    }

    fun getOnlineUsers(): Set<String> = webSocketManager.getOnlineUsers()

    fun getRoomMembers(roomId: String): Set<String> = webSocketManager.getRoomMembers(roomId)

    fun getTypingUsers(roomId: String): Set<String> = webSocketManager.getTypingUsers(roomId)

    fun isUserOnline(userId: String): Boolean = webSocketManager.isUserOnline(userId)
}