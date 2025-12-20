package com.example.plugins

import com.example.config.TenantDatabaseConfig
import com.example.tenant.TenantContext
import com.example.tenant.TenantContextHolder
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.http.*
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import com.example.database.tables.Tenants
import com.example.models.responses.ApiResponse
import io.ktor.server.response.respond
import java.util.*

fun Application.configureTenantResolution() {
    intercept(ApplicationCallPipeline.Call) {
        // Skip tenant resolution for tenant management endpoints
        val path = call.request.path()
        if (path.startsWith("/api/v1/tenants") || path.startsWith("/api/v1/console")) {
            proceed()
            return@intercept
        }

        try {
            val tenant = resolveTenant(call)
            if (tenant != null) {
                TenantContextHolder.setTenant(tenant)
                proceed()
            } else {
                call.respond(ApiResponse<Unit>(
                    success = false,
                    message = "Tenant not found"
                ))
                return@intercept
            }
        } finally {
            TenantContextHolder.clear()
        }
    }
}

private suspend fun resolveTenant(call: ApplicationCall): TenantContext? {
    // Option 1: Resolve by subdomain
//    val host = call.request.host()
//    val subdomain = host.split(".").firstOrNull() ?: return null

    // Option 2: Resolve by header (for testing/API)
    val id = call.request.header("X-Tenant") ?: return null

    return try {
        val tenantId = UUID.fromString(id)
        transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.selectAll()
                .where { Tenants.id eq tenantId }
                .map {
                    TenantContext(
                        id = it[Tenants.id].toString(),
                        name = it[Tenants.name],
                        subDomain = it[Tenants.subDomain],
                        schemaName = it[Tenants.schema_name]
                    )
                }
                .singleOrNull()
        }
    } catch (e: IllegalArgumentException) {
        // Invalid UUID format
        null
    }
}