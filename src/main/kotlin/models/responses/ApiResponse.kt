package com.example.models.responses

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)

@Serializable
data class ErrorResponse(
    val success: Boolean = false,
    val error: ErrorDetail
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)