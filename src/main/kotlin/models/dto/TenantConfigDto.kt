package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class TenantConfigDto(
    val id: Int,
    val tenantId: String,
    val schemaName: String,
    val tenantName: String,
    val subscriptionStatus: String,
    val isPaid: Boolean,
    val subscriptionStartDate: String,
    val subscriptionEndDate: String? = null,
    val planType: String? = null,
    val storageAllocatedMB: Long,
    val storageUsedMB: Long,
    val maxUsers: Int? = null,
    val maxStudents: Int? = null,
    val createdAt: String,
    val updatedAt: String,
    val isActive: Boolean,
    val notes: String? = null,
    val features: List<TenantFeatureDto>? = null
)

@Serializable
data class CreateTenantConfigRequest(
    val tenantId: String,
    val schemaName: String,
    val tenantName: String,
    val subscriptionStatus: String = "TRIAL",
    val isPaid: Boolean = false,
    val planType: String? = null,
    val storageAllocatedMB: Long = 5120, // Default 5GB
    val maxUsers: Int? = null,
    val maxStudents: Int? = null,
    val notes: String? = null
)

@Serializable
data class UpdateTenantConfigRequest(
    val tenantName: String? = null,
    val subscriptionStatus: String? = null,
    val isPaid: Boolean? = null,
    val subscriptionEndDate: String? = null,
    val planType: String? = null,
    val storageAllocatedMB: Long? = null,
    val storageUsedMB: Long? = null,
    val maxUsers: Int? = null,
    val maxStudents: Int? = null,
    val isActive: Boolean? = null,
    val notes: String? = null
)
