package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateTransportStopRequest
import com.example.models.dto.UpdateTransportStopRequest
import com.example.models.responses.ApiResponse
import com.example.services.TransportStopService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class ReorderStopsRequest(
    val stopOrderMap: Map<String, Int>
)

fun Route.transportStopRoutes(transportStopService: TransportStopService) {
    route("/api/v1/transport-stops") {

        // Get all transport stops
        get {
            val stops = transportStopService.getAllTransportStops()
            call.respond(ApiResponse(
                success = true,
                data = stops
            ))
        }

        // Get active transport stops
        get("/active") {
            val stops = transportStopService.getActiveTransportStops()
            call.respond(ApiResponse(
                success = true,
                data = stops
            ))
        }

        // Get transport stops by route ID
        get("/route/{routeId}") {
            val routeId = call.parameters["routeId"]
                ?: throw ApiException("Route ID is required", HttpStatusCode.BadRequest)

            val stops = transportStopService.getTransportStopsByRouteId(routeId)
            call.respond(ApiResponse(
                success = true,
                data = stops
            ))
        }

        // Get active transport stops by route ID
        get("/route/{routeId}/active") {
            val routeId = call.parameters["routeId"]
                ?: throw ApiException("Route ID is required", HttpStatusCode.BadRequest)

            val stops = transportStopService.getActiveTransportStopsByRouteId(routeId)
            call.respond(ApiResponse(
                success = true,
                data = stops
            ))
        }

        // Search transport stops by name
        get("/search") {
            val name = call.request.queryParameters["name"]
                ?: throw ApiException("Name parameter is required", HttpStatusCode.BadRequest)

            val stops = transportStopService.searchTransportStopsByName(name)
            call.respond(ApiResponse(
                success = true,
                data = stops
            ))
        }

        // Get transport stop by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport stop ID is required", HttpStatusCode.BadRequest)

            val stop = transportStopService.getTransportStopById(id)
            call.respond(ApiResponse(
                success = true,
                data = stop
            ))
        }

        // Create transport stop
        post {
            val request = call.receive<CreateTransportStopRequest>()
            val stop = transportStopService.createTransportStop(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = stop,
                message = "Transport stop created successfully"
            ))
        }

        // Update transport stop
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport stop ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateTransportStopRequest>()
            val stop = transportStopService.updateTransportStop(id, request)
            call.respond(ApiResponse(
                success = true,
                data = stop,
                message = "Transport stop updated successfully"
            ))
        }

        // Reorder stops in a route
        put("/route/{routeId}/reorder") {
            val routeId = call.parameters["routeId"]
                ?: throw ApiException("Route ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<ReorderStopsRequest>()
            val stops = transportStopService.reorderStopsInRoute(routeId, request.stopOrderMap)
            call.respond(ApiResponse(
                success = true,
                data = stops,
                message = "Stops reordered successfully"
            ))
        }

        // Toggle transport stop status
        patch("/{id}/toggle-status") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport stop ID is required", HttpStatusCode.BadRequest)

            val stop = transportStopService.toggleTransportStopStatus(id)
            call.respond(ApiResponse(
                success = true,
                data = stop,
                message = "Transport stop status updated successfully"
            ))
        }

        // Delete transport stop
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Transport stop ID is required", HttpStatusCode.BadRequest)

            transportStopService.deleteTransportStop(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Transport stop deleted successfully"
            ))
        }
    }
}