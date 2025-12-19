package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class TenantFeatureDto(
    val id: Int,
    val tenantId: String,
    val featureId: Int?,
    val featureName: String?, // Deprecated, kept for backward compatibility
    val isEnabled: Boolean,
    val customLimitValue: Int?,
    val enabledAt: String?,
    val disabledAt: String?,
    val createdAt: String,
    val updatedAt: String?,
    // Feature details (joined from Features table)
    val feature: FeatureDto? = null
)

@Serializable
data class CreateTenantFeatureRequest(
    val featureId: Int,
    val isEnabled: Boolean = true,
    val customLimitValue: Int? = null
)

@Serializable
data class UpdateTenantFeatureRequest(
    val isEnabled: Boolean? = null,
    val customLimitValue: Int? = null
)

@Serializable
data class AssignFeaturesToTenantRequest(
    val featureIds: List<Int>
)
