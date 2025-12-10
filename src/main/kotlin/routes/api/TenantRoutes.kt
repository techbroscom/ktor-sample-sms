package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateTenantRequest
import com.example.models.responses.ApiResponse
import com.example.services.TenantService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.tenantRoutes(tenantService: TenantService) {
    route("/api/v1/tenants") {

        // Create new tenant
        post {
            val request = call.receive<CreateTenantRequest>()
            val tenant = tenantService.createTenant(request.name, request.subDomain)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = tenant,
                message = "Tenant created successfully"
            ))
        }

        // Get all tenants
        get {
            val tenants = tenantService.getAllTenants()
            call.respond(ApiResponse(
                success = true,
                data = tenants
            ))
        }

        // Get all tenants
        get("/web/{subdomain}") {
            val subDomain = call.parameters["subdomain"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val tenants = tenantService.getTenantBySubdomain(subDomain)
            call.respond(ApiResponse(
                success = true,
                data = tenants
            ))
        }

        // Get current tenant info (for debugging)
        get("/current") {
            val tenant = com.example.tenant.TenantContextHolder.getTenant()
            if (tenant != null) {
                call.respond(ApiResponse(
                    success = true,
                    data = tenant
                ))
            } else {
                throw ApiException("No tenant context found", HttpStatusCode.BadRequest)
            }
        }
    }
}