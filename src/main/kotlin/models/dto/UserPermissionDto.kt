package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserPermissionDto(
    val id: Int,
    val userId: String,
    val tenantId: String,
    val featureId: Int,
    val isEnabled: Boolean,
    val grantedAt: String,
    val grantedBy: String?,
    val createdAt: String,
    val updatedAt: String?,
    // Feature details (joined from Features table)
    val feature: FeatureDto? = null
)

@Serializable
data class AssignUserPermissionsRequest(
    val userId: String,
    val featureIds: List<Int>
)

@Serializable
data class UpdateUserPermissionRequest(
    val isEnabled: Boolean
)

@Serializable
data class BulkAssignPermissionsRequest(
    val userIds: List<String>,
    val featureIds: List<Int>
)
