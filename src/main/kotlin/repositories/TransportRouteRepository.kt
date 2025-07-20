package com.example.repositories

import com.example.database.tables.TransportRoutes
import com.example.models.dto.CreateTransportRouteRequest
import com.example.models.dto.TransportRouteDto
import com.example.models.dto.UpdateTransportRouteRequest
import com.example.utils.tenantDbQuery
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class TransportRouteRepository {

    suspend fun create(request: CreateTransportRouteRequest): UUID = tenantDbQuery {
        val routeId = UUID.randomUUID()
        TransportRoutes.insert {
            it[id] = routeId
            it[name] = request.name
            it[description] = request.description
            it[isActive] = request.isActive
        }
        routeId
    }

    suspend fun findById(id: UUID): TransportRouteDto? = tenantDbQuery {
        TransportRoutes.selectAll()
            .where { TransportRoutes.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<TransportRouteDto> = tenantDbQuery {
        TransportRoutes.selectAll()
            .orderBy(TransportRoutes.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findActive(): List<TransportRouteDto> = tenantDbQuery {
        TransportRoutes.selectAll()
            .where { TransportRoutes.isActive eq true }
            .orderBy(TransportRoutes.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByName(name: String): List<TransportRouteDto> = tenantDbQuery {
        TransportRoutes.selectAll()
            .where { TransportRoutes.name like "%$name%" }
            .orderBy(TransportRoutes.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: UUID, request: UpdateTransportRouteRequest): Boolean = tenantDbQuery {
        TransportRoutes.update({ TransportRoutes.id eq id }) {
            it[name] = request.name
            it[description] = request.description
            it[isActive] = request.isActive
        } > 0
    }

    suspend fun delete(id: UUID): Boolean = tenantDbQuery {
        TransportRoutes.deleteWhere { TransportRoutes.id eq id } > 0
    }

    suspend fun nameExists(name: String): Boolean = tenantDbQuery {
        TransportRoutes.selectAll()
            .where { TransportRoutes.name eq name }
            .count() > 0
    }

    suspend fun nameExistsForOtherRoute(name: String, routeId: UUID): Boolean = tenantDbQuery {
        TransportRoutes.selectAll()
            .where { (TransportRoutes.name eq name) and (TransportRoutes.id neq routeId) }
            .count() > 0
    }

    suspend fun toggleActiveStatus(id: UUID): Boolean = tenantDbQuery {
        val currentStatus = TransportRoutes.selectAll()
            .where { TransportRoutes.id eq id }
            .map { it[TransportRoutes.isActive] }
            .singleOrNull()

        if (currentStatus != null) {
            TransportRoutes.update({ TransportRoutes.id eq id }) {
                it[isActive] = !currentStatus
            } > 0
        } else {
            false
        }
    }

    private fun mapRowToDto(row: ResultRow): TransportRouteDto {
        return TransportRouteDto(
            id = row[TransportRoutes.id].toString(),
            name = row[TransportRoutes.name],
            description = row[TransportRoutes.description],
            isActive = row[TransportRoutes.isActive]
        )
    }
}