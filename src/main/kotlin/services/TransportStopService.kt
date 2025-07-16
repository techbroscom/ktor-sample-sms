package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreateTransportStopRequest
import com.example.models.dto.TransportStopDto
import com.example.models.dto.UpdateTransportStopRequest
import com.example.repositories.TransportStopRepository
import com.example.repositories.TransportRouteRepository
import io.ktor.http.*
import java.math.BigDecimal
import java.util.*

class TransportStopService(
    private val transportStopRepository: TransportStopRepository,
    private val transportRouteRepository: TransportRouteRepository
) {

    suspend fun createTransportStop(request: CreateTransportStopRequest): TransportStopDto {
        validateCreateRequest(request)

        // Verify route exists
        val routeId = UUID.fromString(request.routeId)
        transportRouteRepository.findById(routeId)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        // Check if stop name already exists in this route
        if (transportStopRepository.nameExistsInRoute(routeId, request.name)) {
            throw ApiException("Stop with this name already exists in this route", HttpStatusCode.Conflict)
        }

        val stopId = transportStopRepository.create(request)
        return getTransportStopById(stopId)
    }

    suspend fun getTransportStopById(id: UUID): TransportStopDto {
        return transportStopRepository.findById(id)
            ?: throw ApiException("Transport stop not found", HttpStatusCode.NotFound)
    }

    suspend fun getTransportStopById(id: String): TransportStopDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport stop ID format", HttpStatusCode.BadRequest)
        }
        return getTransportStopById(uuid)
    }

    suspend fun getAllTransportStops(): List<TransportStopDto> {
        return transportStopRepository.findAll()
    }

    suspend fun getTransportStopsByRouteId(routeId: String): List<TransportStopDto> {
        val uuid = try {
            UUID.fromString(routeId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid route ID format", HttpStatusCode.BadRequest)
        }

        // Verify route exists
        transportRouteRepository.findById(uuid)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        return transportStopRepository.findByRouteId(uuid)
    }

    suspend fun getActiveTransportStopsByRouteId(routeId: String): List<TransportStopDto> {
        val uuid = try {
            UUID.fromString(routeId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid route ID format", HttpStatusCode.BadRequest)
        }

        // Verify route exists
        transportRouteRepository.findById(uuid)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        return transportStopRepository.findActiveByRouteId(uuid)
    }

    suspend fun getActiveTransportStops(): List<TransportStopDto> {
        return transportStopRepository.findActive()
    }

    suspend fun searchTransportStopsByName(name: String): List<TransportStopDto> {
        if (name.isBlank()) {
            throw ApiException("Search name cannot be empty", HttpStatusCode.BadRequest)
        }
        return transportStopRepository.findByName(name)
    }

    suspend fun updateTransportStop(id: String, request: UpdateTransportStopRequest): TransportStopDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport stop ID format", HttpStatusCode.BadRequest)
        }

        validateUpdateRequest(request)

        // Verify route exists
        val routeId = UUID.fromString(request.routeId)
        transportRouteRepository.findById(routeId)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        // Check if stop name already exists in this route for other stops
        if (transportStopRepository.nameExistsInRouteForOtherStop(routeId, request.name, uuid)) {
            throw ApiException("Stop with this name already exists in this route", HttpStatusCode.Conflict)
        }

        val updated = transportStopRepository.update(uuid, request)
        if (!updated) {
            throw ApiException("Transport stop not found", HttpStatusCode.NotFound)
        }

        return getTransportStopById(uuid)
    }

    suspend fun deleteTransportStop(id: String) {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport stop ID format", HttpStatusCode.BadRequest)
        }

        val deleted = transportStopRepository.delete(uuid)
        if (!deleted) {
            throw ApiException("Transport stop not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun toggleTransportStopStatus(id: String): TransportStopDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport stop ID format", HttpStatusCode.BadRequest)
        }

        val updated = transportStopRepository.toggleActiveStatus(uuid)
        if (!updated) {
            throw ApiException("Transport stop not found", HttpStatusCode.NotFound)
        }

        return getTransportStopById(uuid)
    }

    suspend fun reorderStopsInRoute(routeId: String, stopOrderMap: Map<String, Int>): List<TransportStopDto> {
        val uuid = try {
            UUID.fromString(routeId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid route ID format", HttpStatusCode.BadRequest)
        }

        // Verify route exists
        transportRouteRepository.findById(uuid)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        // Validate stop order map
        if (stopOrderMap.isEmpty()) {
            throw ApiException("Stop order map cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate all stop IDs are UUIDs
        stopOrderMap.keys.forEach { stopId ->
            try {
                UUID.fromString(stopId)
            } catch (e: IllegalArgumentException) {
                throw ApiException("Invalid stop ID format: $stopId", HttpStatusCode.BadRequest)
            }
        }

        val updated = transportStopRepository.reorderStops(uuid, stopOrderMap)
        if (!updated) {
            throw ApiException("Failed to reorder stops", HttpStatusCode.BadRequest)
        }

        return getTransportStopsByRouteId(routeId)
    }

    private fun validateCreateRequest(request: CreateTransportStopRequest) {
        when {
            request.routeId.isBlank() -> throw ApiException("Route ID cannot be empty", HttpStatusCode.BadRequest)
            request.name.isBlank() -> throw ApiException("Stop name cannot be empty", HttpStatusCode.BadRequest)
            request.name.length > 100 -> throw ApiException("Stop name is too long (max 100 characters)", HttpStatusCode.BadRequest)
            request.orderIndex < 0 -> throw ApiException("Order index must be non-negative", HttpStatusCode.BadRequest)
            request.monthlyFee.isBlank() -> throw ApiException("Monthly fee cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate route ID format
        try {
            UUID.fromString(request.routeId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid route ID format", HttpStatusCode.BadRequest)
        }

        // Validate monthly fee format
        try {
            val fee = BigDecimal(request.monthlyFee)
            if (fee < BigDecimal.ZERO) {
                throw ApiException("Monthly fee must be non-negative", HttpStatusCode.BadRequest)
            }
        } catch (e: NumberFormatException) {
            throw ApiException("Invalid monthly fee format", HttpStatusCode.BadRequest)
        }
    }

    private fun validateUpdateRequest(request: UpdateTransportStopRequest) {
        when {
            request.routeId.isBlank() -> throw ApiException("Route ID cannot be empty", HttpStatusCode.BadRequest)
            request.name.isBlank() -> throw ApiException("Stop name cannot be empty", HttpStatusCode.BadRequest)
            request.name.length > 100 -> throw ApiException("Stop name is too long (max 100 characters)", HttpStatusCode.BadRequest)
            request.orderIndex < 0 -> throw ApiException("Order index must be non-negative", HttpStatusCode.BadRequest)
            request.monthlyFee.isBlank() -> throw ApiException("Monthly fee cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate route ID format
        try {
            UUID.fromString(request.routeId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid route ID format", HttpStatusCode.BadRequest)
        }

        // Validate monthly fee format
        try {
            val fee = BigDecimal(request.monthlyFee)
            if (fee < BigDecimal.ZERO) {
                throw ApiException("Monthly fee must be non-negative", HttpStatusCode.BadRequest)
            }
        } catch (e: NumberFormatException) {
            throw ApiException("Invalid monthly fee format", HttpStatusCode.BadRequest)
        }
    }
}