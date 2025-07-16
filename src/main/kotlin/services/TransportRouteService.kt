package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreateTransportRouteRequest
import com.example.models.dto.TransportRouteDto
import com.example.models.dto.UpdateTransportRouteRequest
import com.example.repositories.TransportRouteRepository
import io.ktor.http.*
import java.util.*

class TransportRouteService(private val transportRouteRepository: TransportRouteRepository) {

    suspend fun createTransportRoute(request: CreateTransportRouteRequest): TransportRouteDto {
        validateCreateRequest(request)

        // Check if route name already exists
        if (transportRouteRepository.nameExists(request.name)) {
            throw ApiException("Transport route with this name already exists", HttpStatusCode.Conflict)
        }

        val routeId = transportRouteRepository.create(request)
        return getTransportRouteById(routeId)
    }

    suspend fun getTransportRouteById(id: UUID): TransportRouteDto {
        return transportRouteRepository.findById(id)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)
    }

    suspend fun getTransportRouteById(id: String): TransportRouteDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport route ID format", HttpStatusCode.BadRequest)
        }
        return getTransportRouteById(uuid)
    }

    suspend fun getAllTransportRoutes(): List<TransportRouteDto> {
        return transportRouteRepository.findAll()
    }

    suspend fun getActiveTransportRoutes(): List<TransportRouteDto> {
        return transportRouteRepository.findActive()
    }

    suspend fun searchTransportRoutesByName(name: String): List<TransportRouteDto> {
        if (name.isBlank()) {
            throw ApiException("Search name cannot be empty", HttpStatusCode.BadRequest)
        }
        return transportRouteRepository.findByName(name)
    }

    suspend fun updateTransportRoute(id: String, request: UpdateTransportRouteRequest): TransportRouteDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport route ID format", HttpStatusCode.BadRequest)
        }

        validateUpdateRequest(request)

        // Check if route name already exists for other routes
        if (transportRouteRepository.nameExistsForOtherRoute(request.name, uuid)) {
            throw ApiException("Transport route with this name already exists", HttpStatusCode.Conflict)
        }

        val updated = transportRouteRepository.update(uuid, request)
        if (!updated) {
            throw ApiException("Transport route not found", HttpStatusCode.NotFound)
        }

        return getTransportRouteById(uuid)
    }

    suspend fun deleteTransportRoute(id: String) {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport route ID format", HttpStatusCode.BadRequest)
        }

        val deleted = transportRouteRepository.delete(uuid)
        if (!deleted) {
            throw ApiException("Transport route not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun toggleTransportRouteStatus(id: String): TransportRouteDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid transport route ID format", HttpStatusCode.BadRequest)
        }

        val updated = transportRouteRepository.toggleActiveStatus(uuid)
        if (!updated) {
            throw ApiException("Transport route not found", HttpStatusCode.NotFound)
        }

        return getTransportRouteById(uuid)
    }

    private fun validateCreateRequest(request: CreateTransportRouteRequest) {
        when {
            request.name.isBlank() -> throw ApiException("Route name cannot be empty", HttpStatusCode.BadRequest)
            request.name.length > 100 -> throw ApiException("Route name is too long (max 100 characters)", HttpStatusCode.BadRequest)
            request.description != null && request.description.length > 1000 -> throw ApiException("Description is too long (max 1000 characters)", HttpStatusCode.BadRequest)
        }
    }

    private fun validateUpdateRequest(request: UpdateTransportRouteRequest) {
        when {
            request.name.isBlank() -> throw ApiException("Route name cannot be empty", HttpStatusCode.BadRequest)
            request.name.length > 100 -> throw ApiException("Route name is too long (max 100 characters)", HttpStatusCode.BadRequest)
            request.description != null && request.description.length > 1000 -> throw ApiException("Description is too long (max 1000 characters)", HttpStatusCode.BadRequest)
        }
    }
}