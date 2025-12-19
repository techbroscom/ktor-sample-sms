package com.example.repositories

import com.example.database.tables.TenantFeatures
import com.example.models.dto.CreateTenantFeatureRequest
import com.example.models.dto.TenantFeatureDto
import com.example.models.dto.UpdateTenantFeatureRequest
import com.example.utils.systemDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TenantFeaturesRepository {

    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    suspend fun create(tenantId: String, request: CreateTenantFeatureRequest): TenantFeatureDto = systemDbQuery {
        val now = LocalDateTime.now()
        val insertedId = TenantFeatures.insert {
            it[TenantFeatures.tenantId] = tenantId
            it[featureName] = request.featureName
            it[isEnabled] = request.isEnabled
            it[createdAt] = now
            it[updatedAt] = now
        } get TenantFeatures.id

        findById(insertedId)!!
    }

    suspend fun findById(id: Int): TenantFeatureDto? = systemDbQuery {
        TenantFeatures.selectAll()
            .where { TenantFeatures.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByTenantId(tenantId: String): List<TenantFeatureDto> = systemDbQuery {
        TenantFeatures.selectAll()
            .where { TenantFeatures.tenantId eq tenantId }
            .orderBy(TenantFeatures.featureName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByTenantIdAndFeatureName(tenantId: String, featureName: String): TenantFeatureDto? = systemDbQuery {
        TenantFeatures.selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.featureName eq featureName)
            }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun update(tenantId: String, featureName: String, request: UpdateTenantFeatureRequest): Boolean = systemDbQuery {
        TenantFeatures.update({
            (TenantFeatures.tenantId eq tenantId) and
                    (TenantFeatures.featureName eq featureName)
        }) {
            it[isEnabled] = request.isEnabled
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(tenantId: String, featureName: String): Boolean = systemDbQuery {
        TenantFeatures.deleteWhere {
            (TenantFeatures.tenantId eq tenantId) and
                    (TenantFeatures.featureName eq featureName)
        } > 0
    }

    suspend fun exists(tenantId: String, featureName: String): Boolean = systemDbQuery {
        TenantFeatures.selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.featureName eq featureName)
            }
            .any()
    }

    private fun mapRowToDto(row: ResultRow): TenantFeatureDto {
        return TenantFeatureDto(
            id = row[TenantFeatures.id],
            tenantId = row[TenantFeatures.tenantId],
            featureName = row[TenantFeatures.featureName],
            isEnabled = row[TenantFeatures.isEnabled],
            createdAt = row[TenantFeatures.createdAt].format(dateFormatter),
            updatedAt = row[TenantFeatures.updatedAt].format(dateFormatter)
        )
    }
}
