package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CommentDto(
    val comment: String,
    val commentedBy: String,
    val commentedAt: String
)