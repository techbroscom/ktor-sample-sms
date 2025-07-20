package com.example.services

import com.example.database.tables.*
import com.example.exceptions.ApiException
import com.example.models.PersonalNotificationRequest
import com.example.models.dto.*
import com.example.repositories.*
import io.ktor.http.*
import java.util.*

class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatRoomMemberRepository: ChatRoomMemberRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val userRepository: UserRepository,
    private val classRepository: ClassRepository,
    private val classSubjectRepository: ClassSubjectRepository,
    private val academicYearRepository: AcademicYearRepository,
    private val notificationService: NotificationService
) {

    // Chat Room Management
    suspend fun createChatRoom(request: CreateChatRoomRequest, createdBy: String): ChatRoomDto {
        validateCreateChatRoomRequest(request)

        val createdByUuid = UUID.fromString(createdBy)
        val academicYearId = UUID.fromString(request.academicYearId)

        // Verify academic year exists
        academicYearRepository.findById(academicYearId)
            ?: throw ApiException("Academic year not found", HttpStatusCode.NotFound)

        // Validate room type specific requirements
        when (request.roomType) {
            "CLASS" -> {
                if (request.classId == null) {
                    throw ApiException("Class ID is required for CLASS room type", HttpStatusCode.BadRequest)
                }
                val classId = (request.classId)
                classRepository.findById(classId)
                    ?: throw ApiException("Class not found", HttpStatusCode.NotFound)

                // Check if room already exists for this class
                val existingRooms = chatRoomRepository.findByClassId(UUID.fromString(classId), academicYearId)
                if (existingRooms.isNotEmpty()) {
                    throw ApiException("Chat room already exists for this class", HttpStatusCode.Conflict)
                }
            }
            "SUBJECT" -> {
                if (request.classSubjectId == null) {
                    throw ApiException("Class Subject ID is required for SUBJECT room type", HttpStatusCode.BadRequest)
                }
                val classSubjectId = request.classSubjectId
                classSubjectRepository.findById(classSubjectId)
                    ?: throw ApiException("Class subject not found", HttpStatusCode.NotFound)

                // Check if room already exists for this class subject
                val existingRoom = chatRoomRepository.findByClassSubjectId(UUID.fromString(classSubjectId), academicYearId)
                if (existingRoom != null) {
                    throw ApiException("Chat room already exists for this class subject", HttpStatusCode.Conflict)
                }
            }
        }

        val roomId = chatRoomRepository.create(request, createdByUuid)

        // Add creator as admin
        chatRoomMemberRepository.addMember(roomId, createdByUuid, ChatMemberRole.ADMIN)

        return getChatRoomById(roomId.toString())
    }

    suspend fun getChatRoomById(id: String): ChatRoomDto {
        val uuid = parseUUID(id, "Invalid chat room ID format")
        return chatRoomRepository.findById(uuid)
            ?: throw ApiException("Chat room not found", HttpStatusCode.NotFound)
    }

    suspend fun getChatRoomsByUserId(userId: String): List<ChatRoomDto> {
        val uuid = parseUUID(userId, "Invalid user ID format")
        return chatRoomRepository.findByUserId(uuid)
    }

    suspend fun updateChatRoom(id: String, request: UpdateChatRoomRequest): ChatRoomDto {
        val uuid = parseUUID(id, "Invalid chat room ID format")

        val updated = chatRoomRepository.update(uuid, request)
        if (!updated) {
            throw ApiException("Chat room not found", HttpStatusCode.NotFound)
        }

        return getChatRoomById(id)
    }

    suspend fun deleteChatRoom(id: String) {
        val uuid = parseUUID(id, "Invalid chat room ID format")

        val deleted = chatRoomRepository.delete(uuid)
        if (!deleted) {
            throw ApiException("Chat room not found", HttpStatusCode.NotFound)
        }
    }

    // Chat Room Member Management
    suspend fun addMembersToRoom(roomId: String, request: AddMemberRequest, requesterId: String): List<ChatRoomMemberDto> {
        val roomUuid = parseUUID(roomId, "Invalid room ID format")
        val requesterUuid = parseUUID(requesterId, "Invalid requester ID format")

        // Verify room exists
        getChatRoomById(roomId)

        // Verify requester has permission (admin or moderator)
        val requesterRole = chatRoomMemberRepository.getMemberRole(roomUuid, requesterUuid)
        if (requesterRole != ChatMemberRole.ADMIN && requesterRole != ChatMemberRole.MODERATOR) {
            throw ApiException("Insufficient permissions to add members", HttpStatusCode.Forbidden)
        }

        val memberRole = ChatMemberRole.valueOf(request.role)
        val userIds = request.userIds.map { parseUUID(it, "Invalid user ID format") }

        // Verify all users exist
        userIds.forEach { userId ->
            userRepository.findById(userId)
                ?: throw ApiException("User not found: $userId", HttpStatusCode.NotFound)
        }

        // Filter out users who are already members
        val newUserIds = userIds.filter { userId ->
            !chatRoomMemberRepository.isMember(roomUuid, userId)
        }

        if (newUserIds.isNotEmpty()) {
            chatRoomMemberRepository.bulkAddMembers(roomUuid, newUserIds, memberRole)
        }

        return chatRoomMemberRepository.findByRoomId(roomUuid)
    }

    suspend fun getRoomMembers(roomId: String): List<ChatRoomMemberDto> {
        val uuid = parseUUID(roomId, "Invalid room ID format")
        return chatRoomMemberRepository.findByRoomId(uuid)
    }

    suspend fun updateMemberRole(roomId: String, userId: String, request: UpdateMemberRoleRequest, requesterId: String): ChatRoomMemberDto {
        val roomUuid = parseUUID(roomId, "Invalid room ID format")
        val userUuid = parseUUID(userId, "Invalid user ID format")
        val requesterUuid = parseUUID(requesterId, "Invalid requester ID format")

        // Verify requester has admin permission
        val requesterRole = chatRoomMemberRepository.getMemberRole(roomUuid, requesterUuid)
        if (requesterRole != ChatMemberRole.ADMIN) {
            throw ApiException("Only admins can update member roles", HttpStatusCode.Forbidden)
        }

        val newRole = ChatMemberRole.valueOf(request.role)
        val updated = chatRoomMemberRepository.updateRole(roomUuid, userUuid, newRole)
        if (!updated) {
            throw ApiException("Member not found", HttpStatusCode.NotFound)
        }

        return chatRoomMemberRepository.findByRoomId(roomUuid).first { it.userId == userId }
    }

    suspend fun removeMemberFromRoom(roomId: String, userId: String, requesterId: String) {
        val roomUuid = parseUUID(roomId, "Invalid room ID format")
        val userUuid = parseUUID(userId, "Invalid user ID format")
        val requesterUuid = parseUUID(requesterId, "Invalid requester ID format")

        // Verify requester has permission (admin or moderator, or removing themselves)
        val requesterRole = chatRoomMemberRepository.getMemberRole(roomUuid, requesterUuid)
        if (requesterUuid != userUuid && requesterRole != ChatMemberRole.ADMIN && requesterRole != ChatMemberRole.MODERATOR) {
            throw ApiException("Insufficient permissions to remove member", HttpStatusCode.Forbidden)
        }

        val removed = chatRoomMemberRepository.removeMember(roomUuid, userUuid)
        if (!removed) {
            throw ApiException("Member not found", HttpStatusCode.NotFound)
        }
    }

    // Chat Message Management
    suspend fun sendMessage(request: SendMessageRequest, senderId: String, webSocketService: ChatWebSocketService? = null): ChatMessageDto {
        validateSendMessageRequest(request)

        val senderUuid = parseUUID(senderId, "Invalid sender ID format")
        val roomUuid = parseUUID(request.roomId, "Invalid room ID format")

        // Verify sender is a member of the room
        if (!chatRoomMemberRepository.isMember(roomUuid, senderUuid)) {
            throw ApiException("You are not a member of this chat room", HttpStatusCode.Forbidden)
        }

        // Verify room exists and is active
        val room = getChatRoomById(request.roomId)
        if (!room.isActive) {
            throw ApiException("Chat room is not active", HttpStatusCode.BadRequest)
        }

        // If replying to a message, verify it exists
        if (request.replyToMessageId != null) {
            val replyToUuid = parseUUID(request.replyToMessageId, "Invalid reply message ID format")
            chatMessageRepository.findById(replyToUuid)
                ?: throw ApiException("Reply message not found", HttpStatusCode.NotFound)
        }

        val messageId = chatMessageRepository.create(request, senderUuid)

        // Update room's last activity
        chatRoomRepository.updateLastActivity(roomUuid)

        val message = getMessageById(messageId.toString())

        // Send notifications to room members (except sender)
        sendChatNotifications(room, message, senderUuid)

        // Broadcast via WebSocket
        webSocketService?.broadcastNewMessage(message)

        return message
    }

    suspend fun getMessageById(id: String): ChatMessageDto {
        val uuid = parseUUID(id, "Invalid message ID format")
        return chatMessageRepository.findById(uuid)
            ?: throw ApiException("Message not found", HttpStatusCode.NotFound)
    }

    suspend fun getRoomMessages(roomId: String, userId: String, limit: Int = 50, offset: Long = 0): List<ChatMessageDto> {
        val roomUuid = parseUUID(roomId, "Invalid room ID format")
        val userUuid = parseUUID(userId, "Invalid user ID format")

        // Verify user is a member of the room
        if (!chatRoomMemberRepository.isMember(roomUuid, userUuid)) {
            throw ApiException("You are not a member of this chat room", HttpStatusCode.Forbidden)
        }

        return chatMessageRepository.findByRoomId(roomUuid, limit, offset)
    }

    suspend fun editMessage(messageId: String, request: EditMessageRequest, userId: String, webSocketService: ChatWebSocketService? = null): ChatMessageDto {
        val messageUuid = parseUUID(messageId, "Invalid message ID format")
        val userUuid = parseUUID(userId, "Invalid user ID format")

        val message = getMessageById(messageId)

        // Verify user is the sender or has admin rights
        if (message.senderId != userId) {
            val roomUuid = parseUUID(message.roomId, "Invalid room ID format")
            val userRole = chatRoomMemberRepository.getMemberRole(roomUuid, userUuid)
            if (userRole != ChatMemberRole.ADMIN) {
                throw ApiException("You can only edit your own messages", HttpStatusCode.Forbidden)
            }
        }

        val updated = chatMessageRepository.update(messageUuid, request)
        if (!updated) {
            throw ApiException("Message not found", HttpStatusCode.NotFound)
        }

        val editedMessage = getMessageById(messageId)

        // Broadcast via WebSocket
        webSocketService?.broadcastMessageEdited(editedMessage)

        return message
    }

    suspend fun deleteMessage(messageId: String, userId: String, webSocketService: ChatWebSocketService? = null) {
        val messageUuid = parseUUID(messageId, "Invalid message ID format")
        val userUuid = parseUUID(userId, "Invalid user ID format")

        val message = getMessageById(messageId)

        // Verify user is the sender or has admin rights
        if (message.senderId != userId) {
            val roomUuid = parseUUID(message.roomId, "Invalid room ID format")
            val userRole = chatRoomMemberRepository.getMemberRole(roomUuid, userUuid)
            if (userRole != ChatMemberRole.ADMIN) {
                throw ApiException("You can only delete your own messages", HttpStatusCode.Forbidden)
            }
        }

        val deleted = chatMessageRepository.delete(messageUuid)
        if (!deleted) {
            throw ApiException("Message not found", HttpStatusCode.NotFound)
        }

        // Broadcast via WebSocket
        webSocketService?.broadcastMessageDeleted(messageId, message.roomId)
    }

    suspend fun markMessagesAsRead(roomId: String, userId: String, webSocketService: ChatWebSocketService? = null) {
        val roomUuid = parseUUID(roomId, "Invalid room ID format")
        val userUuid = parseUUID(userId, "Invalid user ID format")

        // Verify user is a member of the room
        if (!chatRoomMemberRepository.isMember(roomUuid, userUuid)) {
            throw ApiException("You are not a member of this chat room", HttpStatusCode.Forbidden)
        }

        chatRoomMemberRepository.updateLastRead(roomUuid, userUuid)
        webSocketService?.broadcastMessageRead(roomId, userId, System.currentTimeMillis().toString())
    }

    // Auto-create rooms for classes and subjects
    suspend fun autoCreateClassRoom(classId: String, academicYearId: String, createdBy: String): ChatRoomDto {
        val request = CreateChatRoomRequest(
            name = "Class Chat", // Will be updated with actual class name
            description = "General chat for the class",
            roomType = "CLASS",
            classId = classId,
            academicYearId = academicYearId
        )

        return createChatRoom(request, createdBy)
    }

    suspend fun autoCreateSubjectRoom(classSubjectId: String, academicYearId: String, createdBy: String): ChatRoomDto {
        val request = CreateChatRoomRequest(
            name = "Subject Chat", // Will be updated with actual subject name
            description = "Subject-specific discussions",
            roomType = "SUBJECT",
            classSubjectId = classSubjectId,
            academicYearId = academicYearId
        )

        return createChatRoom(request, createdBy)
    }

    // Private helper methods
    private suspend fun sendChatNotifications(room: ChatRoomDto, message: ChatMessageDto, senderUuid: UUID) {
        try {
            val roomMembers = chatRoomMemberRepository.findByRoomId(UUID.fromString(room.id))

            roomMembers.filter { it.userId != senderUuid.toString() }.forEach { member ->
                notificationService.sendChatNotification(
                    userId = member.userId,
                    roomName = room.name,
                    senderName = message.senderName,
                    messageContent = message.content,
                    roomId = room.id
                )
            }
        } catch (e: Exception) {
            // Log error but don't fail the message sending
            println("Failed to send chat notifications: ${e.message}")
        }
    }

    private fun parseUUID(id: String, errorMessage: String): UUID {
        return try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException(errorMessage, HttpStatusCode.BadRequest)
        }
    }

    private fun validateCreateChatRoomRequest(request: CreateChatRoomRequest) {
        when {
            request.name.isBlank() -> throw ApiException("Room name cannot be empty", HttpStatusCode.BadRequest)
            request.name.length > 255 -> throw ApiException("Room name is too long (max 255 characters)", HttpStatusCode.BadRequest)
            request.roomType !in listOf("CLASS", "SUBJECT") -> throw ApiException("Invalid room type", HttpStatusCode.BadRequest)
            request.academicYearId.isBlank() -> throw ApiException("Academic year ID cannot be empty", HttpStatusCode.BadRequest)
        }
    }

    private fun validateSendMessageRequest(request: SendMessageRequest) {
        when {
            request.roomId.isBlank() -> throw ApiException("Room ID cannot be empty", HttpStatusCode.BadRequest)
            request.content.isBlank() && request.fileUrl == null -> throw ApiException("Message content cannot be empty", HttpStatusCode.BadRequest)
            request.content.length > 5000 -> throw ApiException("Message content is too long (max 5000 characters)", HttpStatusCode.BadRequest)
            request.messageType !in listOf("TEXT", "IMAGE", "FILE") -> throw ApiException("Invalid message type", HttpStatusCode.BadRequest)
        }
    }
}

// Extension to NotificationService for chat notifications
suspend fun NotificationService.sendChatNotification(
    userId: String,
    roomName: String,
    senderName: String,
    messageContent: String,
    roomId: String
) {
    val notification = PersonalNotificationRequest(
        title = "$senderName in $roomName",
        body = messageContent.take(100) + if (messageContent.length > 100) "..." else "",
        userId = userId,
        data = mapOf(
            "type" to "chat_message",
            "roomId" to roomId,
            "senderName" to senderName,
            "roomName" to roomName
        )
    )

    fcmService.sendPersonalNotification(notification)
}