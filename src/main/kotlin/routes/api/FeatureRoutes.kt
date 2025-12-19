package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.AssignFeaturesToTenantRequest
import com.example.models.dto.CreateFeatureRequest
import com.example.models.dto.UpdateFeatureRequest
import com.example.models.responses.ApiResponse
import com.example.services.FeatureService
import com.example.services.TenantConfigService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.featureRoutes(
    featureService: FeatureService,
    tenantConfigService: TenantConfigService
) {
    route("/api/v1/admin/features") {

        // Create new feature in catalog
        post {
            val request = call.receive<CreateFeatureRequest>()
            val feature = featureService.createFeature(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = feature,
                message = "Feature created successfully"
            ))
        }

        // Get all features
        get {
            val activeOnly = call.request.queryParameters["activeOnly"]?.toBoolean() ?: false
            val features = featureService.getAllFeatures(activeOnly)
            call.respond(ApiResponse(
                success = true,
                data = features
            ))
        }

        // Get feature by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid feature ID", HttpStatusCode.BadRequest)

            val feature = featureService.getFeatureById(id)
            call.respond(ApiResponse(
                success = true,
                data = feature
            ))
        }

        // Get features by category
        get("/category/{category}") {
            val category = call.parameters["category"]
                ?: throw ApiException("Category is required", HttpStatusCode.BadRequest)

            val features = featureService.getFeaturesByCategory(category)
            call.respond(ApiResponse(
                success = true,
                data = features
            ))
        }

        // Update feature
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid feature ID", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateFeatureRequest>()
            val feature = featureService.updateFeature(id, request)
            call.respond(ApiResponse(
                success = true,
                data = feature,
                message = "Feature updated successfully"
            ))
        }

        // Delete feature
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid feature ID", HttpStatusCode.BadRequest)

            featureService.deleteFeature(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Feature deleted successfully"
            ))
        }
    }

    // Tenant Feature Assignment Routes
    route("/api/v1/admin/tenants/{tenantId}/features") {

        // Assign multiple features to tenant
        post {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<AssignFeaturesToTenantRequest>()
            val features = tenantConfigService.assignMultipleFeaturesToTenant(tenantId, request.featureIds)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = features,
                message = "Features assigned to tenant successfully"
            ))
        }

        // Get all features assigned to tenant
        get {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)

            val features = tenantConfigService.getFeatures(tenantId)
            call.respond(ApiResponse(
                success = true,
                data = features
            ))
        }

        // Remove feature from tenant
        delete("/{featureId}") {
            val tenantId = call.parameters["tenantId"]
                ?: throw ApiException("Tenant ID is required", HttpStatusCode.BadRequest)
            val featureId = call.parameters["featureId"]?.toIntOrNull()
                ?: throw ApiException("Invalid feature ID", HttpStatusCode.BadRequest)

            tenantConfigService.removeTenantFeature(tenantId, featureId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Feature removed from tenant successfully"
            ))
        }
    }
}
