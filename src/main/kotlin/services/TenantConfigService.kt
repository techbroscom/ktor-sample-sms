package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.TenantConfigRepository
import com.example.repositories.TenantFeaturesRepository
import io.ktor.http.*

class TenantConfigService(
    private val tenantConfigRepository: TenantConfigRepository,
    private val tenantFeaturesRepository: TenantFeaturesRepository
) {

    suspend fun createTenant(request: CreateTenantConfigRequest): TenantConfigDto {
        println("=== TenantConfigService.createTenant ===")
        println("Request: $request")

        // Validate tenant doesn't already exist
        println("Checking if tenant exists: ${request.tenantId}")
        val exists = tenantConfigRepository.exists(request.tenantId)
        println("Tenant exists: $exists")

        if (exists) {
            throw ApiException("Tenant with ID '${request.tenantId}' already exists", HttpStatusCode.Conflict)
        }

        // Validate subscription status
        println("Validating subscription status: ${request.subscriptionStatus}")
        validateSubscriptionStatus(request.subscriptionStatus)

        // Create tenant config
        println("Creating tenant config in repository...")
        val result = tenantConfigRepository.create(request)
        println("Tenant config created: $result")
        return result
    }

    suspend fun getTenantById(tenantId: String, includeFeatures: Boolean = true): TenantConfigDto {
        println("=== TenantConfigService.getTenantById ===")
        println("TenantId: $tenantId, includeFeatures: $includeFeatures")

        println("Querying repository for tenant...")
        val tenant = tenantConfigRepository.findByTenantId(tenantId)
        println("Repository result: $tenant")

        if (tenant == null) {
            println("Tenant not found, throwing ApiException")
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        return if (includeFeatures) {
            println("Including features for tenant...")
            val features = tenantFeaturesRepository.findByTenantId(tenantId)
            println("Features found: ${features.size}")
            tenant.copy(features = features)
        } else {
            tenant
        }
    }

    suspend fun getAllTenants(activeOnly: Boolean = false, includeFeatures: Boolean = false): List<TenantConfigDto> {
        val tenants = tenantConfigRepository.findAll(activeOnly)

        return if (includeFeatures) {
            tenants.map { tenant ->
                val features = tenantFeaturesRepository.findByTenantId(tenant.tenantId)
                tenant.copy(features = features)
            }
        } else {
            tenants
        }
    }

    suspend fun updateTenant(tenantId: String, request: UpdateTenantConfigRequest): TenantConfigDto {
        // Validate tenant exists
        if (!tenantConfigRepository.exists(tenantId)) {
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        // Validate subscription status if provided
        request.subscriptionStatus?.let { validateSubscriptionStatus(it) }

        val updated = tenantConfigRepository.update(tenantId, request)
        if (!updated) {
            throw ApiException("Failed to update tenant", HttpStatusCode.InternalServerError)
        }

        return getTenantById(tenantId)
    }

    suspend fun deactivateTenant(tenantId: String): Boolean {
        if (!tenantConfigRepository.exists(tenantId)) {
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        return tenantConfigRepository.delete(tenantId)
    }

    // Feature Management Methods

    suspend fun addFeature(tenantId: String, request: CreateTenantFeatureRequest): TenantFeatureDto {
        // Validate tenant exists
        if (!tenantConfigRepository.exists(tenantId)) {
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        // Check if feature already exists
        if (tenantFeaturesRepository.exists(tenantId, request.featureName)) {
            throw ApiException("Feature '${request.featureName}' already exists for this tenant", HttpStatusCode.Conflict)
        }

        return tenantFeaturesRepository.create(tenantId, request)
    }

    suspend fun getFeatures(tenantId: String): List<TenantFeatureDto> {
        // Validate tenant exists
        if (!tenantConfigRepository.exists(tenantId)) {
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        return tenantFeaturesRepository.findByTenantId(tenantId)
    }

    suspend fun getFeature(tenantId: String, featureName: String): TenantFeatureDto {
        // Validate tenant exists
        if (!tenantConfigRepository.exists(tenantId)) {
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        return tenantFeaturesRepository.findByTenantIdAndFeatureName(tenantId, featureName)
            ?: throw ApiException("Feature not found", HttpStatusCode.NotFound)
    }

    suspend fun updateFeature(tenantId: String, featureName: String, request: UpdateTenantFeatureRequest): TenantFeatureDto {
        // Validate tenant exists
        if (!tenantConfigRepository.exists(tenantId)) {
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        val updated = tenantFeaturesRepository.update(tenantId, featureName, request)
        if (!updated) {
            throw ApiException("Feature not found", HttpStatusCode.NotFound)
        }

        return getFeature(tenantId, featureName)
    }

    suspend fun removeFeature(tenantId: String, featureName: String): Boolean {
        // Validate tenant exists
        if (!tenantConfigRepository.exists(tenantId)) {
            throw ApiException("Tenant not found", HttpStatusCode.NotFound)
        }

        val deleted = tenantFeaturesRepository.delete(tenantId, featureName)
        if (!deleted) {
            throw ApiException("Feature not found", HttpStatusCode.NotFound)
        }

        return true
    }

    suspend fun isFeatureEnabled(tenantId: String, featureName: String): Boolean {
        val feature = tenantFeaturesRepository.findByTenantIdAndFeatureName(tenantId, featureName)
        return feature?.isEnabled == true
    }

    private fun validateSubscriptionStatus(status: String) {
        val validStatuses = listOf("TRIAL", "ACTIVE", "SUSPENDED", "EXPIRED")
        if (status !in validStatuses) {
            throw ApiException(
                "Invalid subscription status. Must be one of: ${validStatuses.joinToString(", ")}",
                HttpStatusCode.BadRequest
            )
        }
    }
}
