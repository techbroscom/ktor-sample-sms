package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.AssignUserPermissionsRequest
import com.example.models.dto.UpdateUserPermissionRequest
import com.example.models.dto.UserPermissionDto
import com.example.repositories.FeatureRepository
import com.example.repositories.TenantFeaturesRepository
import com.example.repositories.UserPermissionsRepository
import com.example.tenant.TenantContextHolder
import io.ktor.http.*
import java.util.*

class UserPermissionService(
    private val userPermissionsRepository: UserPermissionsRepository,
    private val tenantFeaturesRepository: TenantFeaturesRepository,
    private val featureRepository: FeatureRepository
) {

    suspend fun assignPermissionsToUser(
        userId: String,
        request: AssignUserPermissionsRequest,
        grantedBy: String?
    ): List<UserPermissionDto> {
        val userUUID = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid userId format", HttpStatusCode.BadRequest)
        }

        val grantedByUUID = try {
            grantedBy?.let { UUID.fromString(it) }
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid grantedBy format", HttpStatusCode.BadRequest)
        }

        // Get tenantId from context
        val tenantContext = TenantContextHolder.get()
            ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

        // Validate all features exist and are enabled for the tenant
        request.featureIds.forEach { featureId ->
            if (!featureRepository.existsById(featureId)) {
                throw ApiException("Feature with ID $featureId does not exist", HttpStatusCode.BadRequest)
            }

            if (!tenantFeaturesRepository.exists(tenantContext.id, featureId)) {
                throw ApiException("Feature with ID $featureId is not enabled for this tenant", HttpStatusCode.BadRequest)
            }
        }

        return userPermissionsRepository.assignPermissions(userUUID, request.featureIds, grantedByUUID)
    }

    suspend fun getUserPermissions(userId: String): List<UserPermissionDto> {
        val userUUID = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid userId format", HttpStatusCode.BadRequest)
        }

        return userPermissionsRepository.findByUser(userUUID, includeFeature = true)
    }

    suspend fun getEnabledFeatureKeys(userId: String): List<String> {
        val userUUID = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid userId format", HttpStatusCode.BadRequest)
        }

        return userPermissionsRepository.getEnabledFeatureKeys(userUUID)
    }

    suspend fun updatePermission(
        userId: String,
        featureId: Int,
        request: UpdateUserPermissionRequest
    ): UserPermissionDto {
        val userUUID = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid userId format", HttpStatusCode.BadRequest)
        }

        val updated = userPermissionsRepository.update(userUUID, featureId, request)
        if (!updated) {
            throw ApiException("Permission not found", HttpStatusCode.NotFound)
        }

        return userPermissionsRepository.findByUserAndFeature(userUUID, featureId)
            ?: throw ApiException("Permission not found after update", HttpStatusCode.InternalServerError)
    }

    suspend fun removePermission(userId: String, featureId: Int): Boolean {
        val userUUID = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid userId format", HttpStatusCode.BadRequest)
        }

        val deleted = userPermissionsRepository.delete(userUUID, featureId)
        if (!deleted) {
            throw ApiException("Permission not found", HttpStatusCode.NotFound)
        }

        return true
    }

    suspend fun removeAllPermissionsForUser(userId: String): Boolean {
        val userUUID = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid userId format", HttpStatusCode.BadRequest)
        }

        return userPermissionsRepository.deleteAllForUser(userUUID)
    }
}
