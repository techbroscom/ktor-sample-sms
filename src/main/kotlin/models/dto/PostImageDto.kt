package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostImageDto(
    val id: Int? = null,
    val postId: Int,
    val imageUrl: String,
    val imageS3Key: String,
    val displayOrder: Int = 0,
    val createdAt: String? = null
)
