package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeatureDto(
    val id: Int,
    val featureKey: String,
    val name: String,
    val description: String,
    val category: String?,
    val isActive: Boolean,
    val defaultEnabled: Boolean,
    val hasLimit: Boolean,
    val limitType: String?,
    val limitValue: Int?,
    val limitUnit: String?,
    val createdAt: String,
    val updatedAt: String?
)

@Serializable
data class CreateFeatureRequest(
    val featureKey: String,
    val name: String,
    val description: String,
    val category: String? = null,
    val isActive: Boolean = true,
    val defaultEnabled: Boolean = false,
    val hasLimit: Boolean = false,
    val limitType: String? = null,
    val limitValue: Int? = null,
    val limitUnit: String? = null
)

@Serializable
data class UpdateFeatureRequest(
    val name: String? = null,
    val description: String? = null,
    val category: String? = null,
    val isActive: Boolean? = null,
    val defaultEnabled: Boolean? = null,
    val hasLimit: Boolean? = null,
    val limitType: String? = null,
    val limitValue: Int? = null,
    val limitUnit: String? = null
)
