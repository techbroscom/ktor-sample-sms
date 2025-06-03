package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateRulesAndRegulationsRequest
import com.example.models.dto.UpdateRulesAndRegulationsRequest
import com.example.models.responses.ApiResponse
import com.example.services.RulesAndRegulationsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.rulesAndRegulationsRoutes(rulesAndRegulationsService: RulesAndRegulationsService) {
    route("/api/v1/rules-and-regulations") {

        // Get all rules and regulations
        get {
            val rules = rulesAndRegulationsService.getAllRules()
            call.respond(ApiResponse(
                success = true,
                data = rules
            ))
        }

        // Get recent rules and regulations
        get("/recent") {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 10
            val rules = rulesAndRegulationsService.getRecentRules(limit)
            call.respond(ApiResponse(
                success = true,
                data = rules
            ))
        }

        // Search rules and regulations by keyword
        get("/search") {
            val keyword = call.request.queryParameters["keyword"]
                ?: throw ApiException("keyword parameter is required", HttpStatusCode.BadRequest)

            val rules = rulesAndRegulationsService.searchRules(keyword)
            call.respond(ApiResponse(
                success = true,
                data = rules
            ))
        }

        // Get rules and regulations by date range
        get("/date-range") {
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("startDate parameter is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("endDate parameter is required", HttpStatusCode.BadRequest)

            val rules = rulesAndRegulationsService.getRulesByDateRange(startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = rules
            ))
        }

        // Get total count of rules and regulations
        get("/count") {
            val count = rulesAndRegulationsService.getRulesCount()
            call.respond(ApiResponse(
                success = true,
                data = mapOf("count" to count)
            ))
        }

        // Get rule and regulation by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid rule ID", HttpStatusCode.BadRequest)

            val rule = rulesAndRegulationsService.getRuleById(id)
            call.respond(ApiResponse(
                success = true,
                data = rule
            ))
        }

        // Create rule and regulation
        post {
            val request = call.receive<CreateRulesAndRegulationsRequest>()
            val rule = rulesAndRegulationsService.createRule(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = rule,
                message = "Rule created successfully"
            ))
        }

        // Update rule and regulation
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid rule ID", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateRulesAndRegulationsRequest>()
            val rule = rulesAndRegulationsService.updateRule(id, request)
            call.respond(ApiResponse(
                success = true,
                data = rule,
                message = "Rule updated successfully"
            ))
        }

        // Delete rule and regulation
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid rule ID", HttpStatusCode.BadRequest)

            rulesAndRegulationsService.deleteRule(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Rule deleted successfully"
            ))
        }
    }
}