package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.AssignUserPermissionsRequest
import com.example.models.dto.UpdateUserPermissionRequest
import com.example.models.responses.ApiResponse
import com.example.services.UserPermissionService
import com.example.tenant.TenantContextHolder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userPermissionRoutes(userPermissionService: UserPermissionService) {
    route("/api/v1/users/{userId}/permissions") {

        // Assign permissions to a user (staff)
        post {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val tenantContext = TenantContextHolder.getTenant()
                ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

            val request = call.receive<AssignUserPermissionsRequest>()

            // TODO: Get grantedBy from authenticated user session
            val grantedBy = null // Replace with actual authenticated admin user ID

            val permissions = userPermissionService.assignPermissionsToUser(
                userId = userId,
                request = request,
                grantedBy = grantedBy
            )

            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = permissions,
                message = "Permissions assigned successfully"
            ))
        }

        // Get all permissions for a user
        get {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val tenantContext = TenantContextHolder.getTenant()
                ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

            val permissions = userPermissionService.getUserPermissions(userId)
            call.respond(ApiResponse(
                success = true,
                data = permissions
            ))
        }

        // Get enabled feature keys for a user
        get("/enabled") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val tenantContext = TenantContextHolder.getTenant()
                ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

            val featureKeys = userPermissionService.getEnabledFeatureKeys(userId)
            call.respond(ApiResponse(
                success = true,
                data = featureKeys
            ))
        }

        // Update a specific permission
        put("/{featureId}") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)
            val featureId = call.parameters["featureId"]?.toIntOrNull()
                ?: throw ApiException("Invalid feature ID", HttpStatusCode.BadRequest)

            val tenantContext = TenantContextHolder.getTenant()
                ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateUserPermissionRequest>()
            val permission = userPermissionService.updatePermission(
                userId = userId,
                featureId = featureId,
                request = request
            )

            call.respond(ApiResponse(
                success = true,
                data = permission,
                message = "Permission updated successfully"
            ))
        }

        // Remove a specific permission
        delete("/{featureId}") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)
            val featureId = call.parameters["featureId"]?.toIntOrNull()
                ?: throw ApiException("Invalid feature ID", HttpStatusCode.BadRequest)

            val tenantContext = TenantContextHolder.getTenant()
                ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

            userPermissionService.removePermission(userId, featureId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Permission removed successfully"
            ))
        }

        // Remove all permissions for a user
        delete {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val tenantContext = TenantContextHolder.getTenant()
                ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

            userPermissionService.removeAllPermissionsForUser(userId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "All permissions removed successfully"
            ))
        }
    }

    // Route to get available features for assigning to staff
    route("/api/v1/features/available") {
        get {
            val tenantContext = TenantContextHolder.getTenant()
                ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

            // This endpoint is handled by tenantConfigService
            // Returns features enabled for the current tenant
            call.respond(ApiResponse(
                success = true,
                data = emptyList<String>(),
                message = "Use /api/v1/tenantsConfig/{tenantId}/features to get available features"
            ))
        }
    }
}
