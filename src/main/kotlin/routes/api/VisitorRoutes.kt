package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.VisitorService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.visitorRoutes(visitorService: VisitorService) {
    route("/api/v1/visitors") {

        // Create new visitor (ADMIN, STAFF)
        post {
            val request = call.receive<CreateVisitorRequest>()
            // TODO: Extract userId from JWT token when authentication is implemented
            // For now, using a placeholder - this should be replaced with actual JWT authentication
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val visitor = visitorService.createVisitor(request, userId)
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    data = visitor,
                    message = "Visitor created successfully"
                )
            )
        }

        // Search/List visitors with filters
        get {
            val searchRequest = VisitorSearchRequest(
                searchQuery = call.request.queryParameters["search"],
                visitDate = call.request.queryParameters["visitDate"],
                status = call.request.queryParameters["status"],
                hostUserId = call.request.queryParameters["hostUserId"],
                fromDate = call.request.queryParameters["fromDate"],
                toDate = call.request.queryParameters["toDate"],
                page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1,
                pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            )

            val result = visitorService.searchVisitors(searchRequest)
            call.respond(result)
        }

        // Get visitor statistics
        get("/stats") {
            val fromDate = call.request.queryParameters["fromDate"]
            val toDate = call.request.queryParameters["toDate"]

            val stats = visitorService.getVisitorStats(fromDate, toDate)
            call.respond(ApiResponse(
                success = true,
                data = stats
            ))
        }

        // Get visitors hosted by current user
        get("/my-hosted") {
            // TODO: Extract userId from JWT token when authentication is implemented
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val status = call.request.queryParameters["status"]
            val visitors = visitorService.getMyHostedVisitors(userId, status)
            call.respond(ApiResponse(
                success = true,
                data = visitors
            ))
        }

        // Get visitor by ID
        get("/{id}") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val visitor = visitorService.getVisitor(visitorId)
            call.respond(ApiResponse(
                success = true,
                data = visitor
            ))
        }

        // Update visitor
        put("/{id}") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateVisitorRequest>()
            val visitor = visitorService.updateVisitor(visitorId, request)
            call.respond(ApiResponse(
                success = true,
                data = visitor,
                message = "Visitor updated successfully"
            ))
        }

        // Check in visitor
        post("/{id}/check-in") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val request = call.receiveNullable<CheckInRequest>() ?: CheckInRequest()
            val visitor = visitorService.checkIn(visitorId, request)
            call.respond(ApiResponse(
                success = true,
                data = visitor,
                message = "Visitor checked in successfully"
            ))
        }

        // Check out visitor
        post("/{id}/check-out") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val request = call.receiveNullable<CheckOutRequest>() ?: CheckOutRequest()
            val visitor = visitorService.checkOut(visitorId, request)
            call.respond(ApiResponse(
                success = true,
                data = visitor,
                message = "Visitor checked out successfully"
            ))
        }

        // Cancel visitor
        post("/{id}/cancel") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val visitor = visitorService.cancelVisitor(visitorId)
            call.respond(ApiResponse(
                success = true,
                data = visitor,
                message = "Visitor cancelled successfully"
            ))
        }

        // Delete visitor (only SCHEDULED or CANCELLED)
        delete("/{id}") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            visitorService.deleteVisitor(visitorId)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Unit>(
                    success = true,
                    data = null,
                    message = "Visitor deleted successfully"
                )
            )
        }
    }
}
