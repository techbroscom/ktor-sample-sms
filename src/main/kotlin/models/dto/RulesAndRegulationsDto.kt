package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class RulesAndRegulationsDto(
    val id: Int? = null,
    val rule: String,
    val createdAt: String, // Format: YYYY-MM-DDTHH:mm:ss
    val updatedAt: String  // Format: YYYY-MM-DDTHH:mm:ss
)

@Serializable
data class CreateRulesAndRegulationsRequest(
    val rule: String
)

@Serializable
data class UpdateRulesAndRegulationsRequest(
    val rule: String
)