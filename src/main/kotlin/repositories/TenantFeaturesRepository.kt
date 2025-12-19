package com.example.repositories

import com.example.database.tables.Features
import com.example.database.tables.TenantFeatures
import com.example.models.dto.*
import com.example.utils.systemDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TenantFeaturesRepository(
    private val featureRepository: FeatureRepository
) {

    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    suspend fun create(tenantId: String, request: CreateTenantFeatureRequest): TenantFeatureDto = systemDbQuery {
        val now = LocalDateTime.now()
        val insertedId = TenantFeatures.insert {
            it[TenantFeatures.tenantId] = tenantId
            it[featureId] = request.featureId
            it[featureName] = null // Deprecated
            it[isEnabled] = request.isEnabled
            it[customLimitValue] = request.customLimitValue
            it[enabledAt] = if (request.isEnabled) now else null
            it[disabledAt] = null
            it[createdAt] = now
            it[updatedAt] = null
        } get TenantFeatures.id

        findById(insertedId)!!
    }

    suspend fun findById(id: Int, includeFeature: Boolean = true): TenantFeatureDto? = systemDbQuery {
        val result = TenantFeatures.selectAll()
            .where { TenantFeatures.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()

        if (result != null && includeFeature && result.featureId != null) {
            val feature = featureRepository.findById(result.featureId)
            result.copy(feature = feature)
        } else {
            result
        }
    }

    suspend fun findByTenantId(tenantId: String, includeFeature: Boolean = true): List<TenantFeatureDto> = systemDbQuery {
        val results = TenantFeatures.selectAll()
            .where { TenantFeatures.tenantId eq tenantId }
            .orderBy(TenantFeatures.createdAt to SortOrder.ASC)
            .map { mapRowToDto(it) }

        if (includeFeature) {
            results.map { result ->
                if (result.featureId != null) {
                    val feature = featureRepository.findById(result.featureId)
                    result.copy(feature = feature)
                } else {
                    result
                }
            }
        } else {
            results
        }
    }

    suspend fun findByTenantIdAndFeatureId(tenantId: String, featureId: Int, includeFeature: Boolean = true): TenantFeatureDto? = systemDbQuery {
        val result = TenantFeatures.selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.featureId eq featureId)
            }
            .map { mapRowToDto(it) }
            .singleOrNull()

        if (result != null && includeFeature && result.featureId != null) {
            val feature = featureRepository.findById(result.featureId)
            result.copy(feature = feature)
        } else {
            result
        }
    }

    // Legacy method - deprecated but kept for backward compatibility
    suspend fun findByTenantIdAndFeatureName(tenantId: String, featureName: String): TenantFeatureDto? = systemDbQuery {
        TenantFeatures.selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.featureName eq featureName)
            }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun update(tenantId: String, featureId: Int, request: UpdateTenantFeatureRequest): Boolean = systemDbQuery {
        val now = LocalDateTime.now()
        TenantFeatures.update({
            (TenantFeatures.tenantId eq tenantId) and
                    (TenantFeatures.featureId eq featureId)
        }) {
            request.isEnabled?.let { enabled ->
                it[isEnabled] = enabled
                if (enabled) {
                    it[enabledAt] = now
                    it[disabledAt] = null
                } else {
                    it[disabledAt] = now
                }
            }
            request.customLimitValue?.let { value -> it[customLimitValue] = value }
            it[updatedAt] = now
        } > 0
    }

    suspend fun delete(tenantId: String, featureId: Int): Boolean = systemDbQuery {
        TenantFeatures.deleteWhere {
            (TenantFeatures.tenantId eq tenantId) and
                    (TenantFeatures.featureId eq featureId)
        } > 0
    }

    suspend fun exists(tenantId: String, featureId: Int): Boolean = systemDbQuery {
        TenantFeatures.selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.featureId eq featureId)
            }
            .any()
    }

    // Get enabled feature keys for a tenant
    suspend fun getEnabledFeatureKeys(tenantId: String): List<String> = systemDbQuery {
        TenantFeatures.innerJoin(Features, { featureId }, { Features.id })
            .selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.isEnabled eq true) and
                        (Features.isActive eq true)
            }
            .map { it[Features.featureKey] }
    }

    // Get enabled features with full details for a tenant
    suspend fun getEnabledFeatures(tenantId: String): List<TenantFeatureDto> = systemDbQuery {
        val results = TenantFeatures.selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.isEnabled eq true)
            }
            .orderBy(TenantFeatures.createdAt to SortOrder.ASC)
            .map { mapRowToDto(it) }

        results.map { result ->
            if (result.featureId != null) {
                val feature = featureRepository.findById(result.featureId)
                result.copy(feature = feature)
            } else {
                result
            }
        }
    }

    private fun mapRowToDto(row: ResultRow): TenantFeatureDto {
        return TenantFeatureDto(
            id = row[TenantFeatures.id],
            tenantId = row[TenantFeatures.tenantId],
            featureId = row[TenantFeatures.featureId],
            featureName = row[TenantFeatures.featureName],
            isEnabled = row[TenantFeatures.isEnabled],
            customLimitValue = row[TenantFeatures.customLimitValue],
            enabledAt = row[TenantFeatures.enabledAt]?.format(dateFormatter),
            disabledAt = row[TenantFeatures.disabledAt]?.format(dateFormatter),
            createdAt = row[TenantFeatures.createdAt].format(dateFormatter),
            updatedAt = row[TenantFeatures.updatedAt]?.format(dateFormatter),
            feature = null // Will be populated by caller if needed
        )
    }
}
