package com.example.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class PaginatedResponse<T>(
    val success: Boolean,
    val data: T,
    val pagination: PaginationInfo,
    val message: String? = null
) {
    @Serializable
    data class PaginationInfo(
        val page: Int,
        val pageSize: Int,
        val totalItems: Long,
        val totalPages: Long
    )
}