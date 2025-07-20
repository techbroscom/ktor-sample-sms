package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChatRoomDto(
    val id: String,
    val name: String,
    val description: String?,
    val roomType: String,
    val classId: String?,
    val className: String?, // Include class name for convenience
    val classSubjectId: String?,
    val subjectName: String?, // Include subject name for convenience
    val academicYearId: String,
    val academicYearName: String,
    val createdBy: String,
    val createdByName: String,
    val isActive: Boolean,
    val lastActivityAt: String,
    val memberCount: Int,
    val unreadCount: Int = 0,
    val createdAt: String
)

@Serializable
data class CreateChatRoomRequest(
    val name: String,
    val description: String? = null,
    val roomType: String, // CLASS or SUBJECT
    val classId: String? = null,
    val classSubjectId: String? = null,
    val academicYearId: String
)

@Serializable
data class UpdateChatRoomRequest(
    val name: String,
    val description: String? = null,
    val isActive: Boolean
)

@Serializable
data class ChatRoomMemberDto(
    val id: String,
    val roomId: String,
    val userId: String,
    val userName: String,
    val userRole: String, // User's system role (ADMIN, STAFF, STUDENT)
    val chatRole: String, // Chat room role (ADMIN, MODERATOR, MEMBER)
    val joinedAt: String,
    val lastReadAt: String?,
    val isActive: Boolean
)

@Serializable
data class AddMemberRequest(
    val userIds: List<String>,
    val role: String = "MEMBER"
)

@Serializable
data class UpdateMemberRoleRequest(
    val role: String // ADMIN, MODERATOR, MEMBER
)

@Serializable
data class ChatMessageDto(
    val id: String,
    val roomId: String,
    val senderId: String,
    val senderName: String,
    val messageType: String,
    val content: String,
    val fileUrl: String?,
    val fileName: String?,
    val replyToMessageId: String?,
    val replyToContent: String?, // Include replied message content
    val isEdited: Boolean,
    val isDeleted: Boolean,
    val sentAt: String,
    val editedAt: String?
)

@Serializable
data class SendMessageRequest(
    val roomId: String,
    val messageType: String = "TEXT",
    val content: String,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val replyToMessageId: String? = null
)

@Serializable
data class EditMessageRequest(
    val content: String
)