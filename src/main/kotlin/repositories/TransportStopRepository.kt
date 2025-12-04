package com.example.repositories

import com.example.database.tables.TransportRoutes
import com.example.database.tables.TransportStops
import com.example.models.dto.CreateTransportStopRequest
import com.example.models.dto.TransportStopDto
import com.example.models.dto.UpdateTransportStopRequest
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.util.*

class TransportStopRepository {

    suspend fun create(request: CreateTransportStopRequest): UUID = tenantDbQuery {
        val stopId = UUID.randomUUID()
        TransportStops.insert {
            it[id] = stopId
            it[routeId] = UUID.fromString(request.routeId)
            it[name] = request.name
            it[orderIndex] = request.orderIndex
            it[monthlyFee] = BigDecimal(request.monthlyFee)
            it[isActive] = request.isActive
        }
        stopId
    }

    suspend fun findById(id: UUID): TransportStopDto? = tenantDbQuery {
        TransportStops.join(
            TransportRoutes,
            JoinType.INNER,
            TransportStops.routeId,
            TransportRoutes.id
        ).selectAll()
            .where { TransportStops.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<TransportStopDto> = tenantDbQuery {
        TransportStops.join(
            TransportRoutes,
            JoinType.INNER,
            TransportStops.routeId,
            TransportRoutes.id
        ).selectAll()
            .orderBy(TransportRoutes.name to SortOrder.ASC, TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByRouteId(routeId: UUID): List<TransportStopDto> = tenantDbQuery {
        TransportStops.join(
            TransportRoutes,
            JoinType.INNER,
            TransportStops.routeId,
            TransportRoutes.id
        ).selectAll()
            .where { TransportStops.routeId eq routeId }
            .orderBy(TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findActiveByRouteId(routeId: UUID): List<TransportStopDto> = tenantDbQuery {
        TransportStops.join(
            TransportRoutes,
            JoinType.INNER,
            TransportStops.routeId,
            TransportRoutes.id
        ).selectAll()
            .where { (TransportStops.routeId eq routeId) and (TransportStops.isActive eq true) }
            .orderBy(TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findActive(): List<TransportStopDto> = tenantDbQuery {
        TransportStops.join(
            TransportRoutes,
            JoinType.INNER,
            TransportStops.routeId,
            TransportRoutes.id
        ).selectAll()
            .where { (TransportStops.isActive eq true) and (TransportRoutes.isActive eq true) }
            .orderBy(TransportRoutes.name to SortOrder.ASC, TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByName(name: String): List<TransportStopDto> = tenantDbQuery {
        TransportStops.join(
            TransportRoutes,
            JoinType.INNER,
            TransportStops.routeId,
            TransportRoutes.id
        ).selectAll()
            .where { TransportStops.name like "%$name%" }
            .orderBy(TransportRoutes.name to SortOrder.ASC, TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: UUID, request: UpdateTransportStopRequest): Boolean = tenantDbQuery {
        TransportStops.update({ TransportStops.id eq id }) {
            it[routeId] = UUID.fromString(request.routeId)
            it[name] = request.name
            it[orderIndex] = request.orderIndex
            it[monthlyFee] = BigDecimal(request.monthlyFee)
            it[isActive] = request.isActive
        } > 0
    }

    suspend fun delete(id: UUID): Boolean = tenantDbQuery {
        TransportStops.deleteWhere { TransportStops.id eq id } > 0
    }

    suspend fun nameExistsInRoute(routeId: UUID, name: String): Boolean = tenantDbQuery {
        TransportStops.selectAll()
            .where { (TransportStops.routeId eq routeId) and (TransportStops.name eq name) }
            .count() > 0
    }

    suspend fun nameExistsInRouteForOtherStop(routeId: UUID, name: String, stopId: UUID): Boolean = tenantDbQuery {
        TransportStops.selectAll()
            .where {
                (TransportStops.routeId eq routeId) and
                        (TransportStops.name eq name) and
                        (TransportStops.id neq stopId)
            }
            .count() > 0
    }

    suspend fun toggleActiveStatus(id: UUID): Boolean = tenantDbQuery {
        val currentStatus = TransportStops.selectAll()
            .where { TransportStops.id eq id }
            .map { it[TransportStops.isActive] }
            .singleOrNull()

        if (currentStatus != null) {
            TransportStops.update({ TransportStops.id eq id }) {
                it[isActive] = !currentStatus
            } > 0
        } else {
            false
        }
    }

    suspend fun getMaxOrderIndexForRoute(routeId: UUID): Int = tenantDbQuery {
        TransportStops.selectAll()
            .where { TransportStops.routeId eq routeId }
            .maxOfOrNull { it[TransportStops.orderIndex] } ?: 0
    }

    suspend fun reorderStops(routeId: UUID, stopOrderMap: Map<String, Int>): Boolean = tenantDbQuery {
        var success = true
        stopOrderMap.forEach { (stopId, newOrder) ->
            val updated = TransportStops.update({
                (TransportStops.id eq UUID.fromString(stopId)) and
                        (TransportStops.routeId eq routeId)
            }) {
                it[orderIndex] = newOrder
            }
            if (updated == 0) success = false
        }
        success
    }

    private fun mapRowToDto(row: ResultRow): TransportStopDto {
        return TransportStopDto(
            id = row[TransportStops.id].toString(),
            routeId = row[TransportStops.routeId].toString(),
            routeName = row[TransportRoutes.name],
            name = row[TransportStops.name],
            orderIndex = row[TransportStops.orderIndex],
            monthlyFee = row[TransportStops.monthlyFee].toString(),
            isActive = row[TransportStops.isActive]
        )
    }
}