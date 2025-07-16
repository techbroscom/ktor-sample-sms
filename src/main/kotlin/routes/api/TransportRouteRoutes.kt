package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateTransportRouteRequest
import com.example.models.dto.UpdateTransportRouteRequest
import com.example.models.responses.ApiResponse
import com.example.services.TransportRouteService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.transportRouteRoutes(transportRouteService: TransportRouteService) {
    route("/api/v1/transport-routes") {

        // Get all transport routes
        get {
            val routes = transportRouteService.getAllTransportRoutes()
            call.respond(ApiResponse(
                success = true,
                data = routes
            ))
        }

        // Get active transport routes
        get("/active") {
            val routes = transportRouteService.getActiveTransportRoutes()
            call.respond(ApiResponse(
                success = true,
                data = routes
            ))
        }

        // Search transport routes by name
        get("/search") {
            val name = call.request.queryParameters["name"]
                ?: throw ApiException("Name parameter is required", HttpStatusCode.BadRequest)

            val routes = transportRouteService.searchTransportRoutesByName(name)
            call.respond(ApiResponse(
                success = true,
                data = routes
            ))
        }

        // Get transport route by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport route ID is required", HttpStatusCode.BadRequest)

            val route = transportRouteService.getTransportRouteById(id)
            call.respond(ApiResponse(
                success = true,
                data = route
            ))
        }

        // Create transport route
        post {
            val request = call.receive<CreateTransportRouteRequest>()
            val route = transportRouteService.createTransportRoute(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = route,
                message = "Transport route created successfully"
            ))
        }

        // Update transport route
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport route ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateTransportRouteRequest>()
            val route = transportRouteService.updateTransportRoute(id, request)
            call.respond(ApiResponse(
                success = true,
                data = route,
                message = "Transport route updated successfully"
            ))
        }

        // Toggle transport route status
        patch("/{id}/toggle-status") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport route ID is required", HttpStatusCode.BadRequest)

            val route = transportRouteService.toggleTransportRouteStatus(id)
            call.respond(ApiResponse(
                success = true,
                data = route,
                message = "Transport route status updated successfully"
            ))
        }

        // Delete transport route
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport route ID is required", HttpStatusCode.BadRequest)

            transportRouteService.deleteTransportRoute(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Transport route deleted successfully"
            ))
        }
    }
}