package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class TenantFeatureDto(
    val id: Int,
    val tenantId: String,
    val featureName: String,
    val isEnabled: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CreateTenantFeatureRequest(
    val featureName: String,
    val isEnabled: Boolean = true
)

@Serializable
data class UpdateTenantFeatureRequest(
    val isEnabled: Boolean
)
