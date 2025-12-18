package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class PostDto(
    val id: Int? = null,
    val title: String,
    val content: String,
    val author: String? = null,
    val images: List<PostImageDto> = emptyList(),
    val createdAt: String // Format: ISO DateTime string
)

@Serializable
data class CreatePostRequest(
    val title: String,
    val content: String,
    val author: String? = null
)

@Serializable
data class UpdatePostRequest(
    val title: String,
    val content: String,
    val author: String? = null
)