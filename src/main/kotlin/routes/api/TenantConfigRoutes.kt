package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateTenantConfigRequest
import com.example.models.dto.CreateTenantFeatureRequest
import com.example.models.dto.UpdateTenantConfigRequest
import com.example.models.dto.UpdateTenantFeatureRequest
import com.example.models.responses.ApiResponse
import com.example.services.TenantConfigService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tenantConfigRoutes(tenantConfigService: TenantConfigService) {
    route("/api/v1/tenantsConfig") {

        // Create new tenant config
        post {
            println("=== TenantConfigRoutes: POST /api/v1/tenantsConfig received ===")
            val request = call.receive<CreateTenantConfigRequest>()
            println("Request received: $request")
            val tenant = tenantConfigService.createTenant(request)
            println("Tenant created successfully: $tenant")
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = tenant,
                message = "Tenant created successfully"
            ))
        }

        // Get all tenants
        get {
            val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: false
            val includeFeatures = call.request.queryParameters["includeFeatures"]?.toBoolean() ?: false

            val tenants = tenantConfigService.getAllTenants(activeOnly, includeFeatures)
            call.respond(ApiResponse(
                success = true,
                data = tenants
            ))
        }

        // Get tenant by ID
        get("/{tenantId}") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)

            println("=== TenantConfigRoutes: GET /api/v1/tenantsConfig/$tenantId ===")
            val includeFeatures = call.request.queryParameters["includeFeatures"]?.toBoolean() ?: true
            println("Query params - includeFeatures: $includeFeatures")

            val tenant = tenantConfigService.getTenantById(tenantId, includeFeatures)
            println("Tenant retrieved: $tenant")
            call.respond(ApiResponse(
                success = true,
                data = tenant
            ))
        }

        // Update tenant
        put("/{tenantId}") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateTenantConfigRequest>()
            val tenant = tenantConfigService.updateTenant(tenantId, request)
            call.respond(ApiResponse(
                success = true,
                data = tenant,
                message = "Tenant updated successfully"
            ))
        }

        // Deactivate tenant
        delete("/{tenantId}") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)

            tenantConfigService.deactivateTenant(tenantId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Tenant deactivated successfully"
            ))
        }

        // Feature Management Routes

        // Get all features for a tenant
        get("/{tenantId}/features") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)

            val features = tenantConfigService.getFeatures(tenantId)
            call.respond(ApiResponse(
                success = true,
                data = features
            ))
        }

        // Add feature to tenant
        post("/{tenantId}/features") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<CreateTenantFeatureRequest>()
            val feature = tenantConfigService.addFeature(tenantId, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = feature,
                message = "Feature added successfully"
            ))
        }

        // Get specific feature
        get("/{tenantId}/features/{featureName}") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)
            val featureName = call.parameters["featureName"]
                ?: throw ApiException("Feature name is required", HttpStatusCode.BadRequest)

            val feature = tenantConfigService.getFeature(tenantId, featureName)
            call.respond(ApiResponse(
                success = true,
                data = feature
            ))
        }

        // Update feature (toggle enabled/disabled)
        put("/{tenantId}/features/{featureName}") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)
            val featureName = call.parameters["featureName"]
                ?: throw ApiException("Feature name is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateTenantFeatureRequest>()
            val feature = tenantConfigService.updateFeature(tenantId, featureName, request)
            call.respond(ApiResponse(
                success = true,
                data = feature,
                message = "Feature updated successfully"
            ))
        }

        // Remove feature
        delete("/{tenantId}/features/{featureName}") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)
            val featureName = call.parameters["featureName"]
                ?: throw ApiException("Feature name is required", HttpStatusCode.BadRequest)

            tenantConfigService.removeFeature(tenantId, featureName)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Feature removed successfully"
            ))
        }
    }
}
