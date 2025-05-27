package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ComplaintDto(
    val id: String,
    val title: String,
    val content: String,
    val author: String,
    val category: String,
    val status: String,
    val isAnonymous: Boolean,
    val createdAt: String,
    val comments: List<CommentDto>
)

@Serializable
data class CreateComplaintRequest(
    val title: String,
    val content: String,
    val author: String,
    val category: String,
    val isAnonymous: Boolean = false
)

@Serializable
data class UpdateComplaintRequest(
    val title: String,
    val content: String,
    val category: String,
    val status: String
)

@Serializable
data class UpdateStatusRequest(
    val status: String
)

@Serializable
data class AddCommentRequest(
    val comment: String,
    val commentedBy: String
)