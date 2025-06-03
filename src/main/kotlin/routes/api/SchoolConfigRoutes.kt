package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.UpdateSchoolConfigRequest
import com.example.models.responses.ApiResponse
import com.example.services.SchoolConfigService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.schoolConfigRoutes(schoolConfigService: SchoolConfigService) {
    route("/api/v1/school-config") {

        // Get all school configurations
        get {
            val configs = schoolConfigService.getAllSchoolConfigs()
            call.respond(ApiResponse(
                success = true,
                data = configs
            ))
        }


        // Get school configuration by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid school configuration ID", HttpStatusCode.BadRequest)

            val config = schoolConfigService.getSchoolConfigById(id)
            call.respond(ApiResponse(
                success = true,
                data = config
            ))
        }


        // Update school configuration
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid school configuration ID", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateSchoolConfigRequest>()
            val config = schoolConfigService.updateSchoolConfig(id, request)
            call.respond(ApiResponse(
                success = true,
                data = config,
                message = "School configuration updated successfully"
            ))
        }
    }
}