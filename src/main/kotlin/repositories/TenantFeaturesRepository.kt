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

    /* =========================================================
     * CREATE
     * ========================================================= */

    suspend fun create(
        tenantId: String,
        request: CreateTenantFeatureRequest
    ): TenantFeatureDto =
        systemDbQuery {

            val now = LocalDateTime.now()

            val id = TenantFeatures.insert {
                it[TenantFeatures.tenantId] = tenantId
                it[featureId] = request.featureId
                it[featureName] = null // deprecated
                it[isEnabled] = request.isEnabled
                it[customLimitValue] = request.customLimitValue
                it[enabledAt] = if (request.isEnabled) now else null
                it[disabledAt] = null
                it[createdAt] = now
                it[updatedAt] = null
            } get TenantFeatures.id

            findByIdTx(id, includeFeature = true)!!
        }

    /* =========================================================
     * READ (PUBLIC)
     * ========================================================= */

    suspend fun findById(
        id: Int,
        includeFeature: Boolean = true
    ): TenantFeatureDto? =
        systemDbQuery {
            findByIdTx(id, includeFeature)
        }

    suspend fun findByTenantId(
        tenantId: String,
        includeFeature: Boolean = true
    ): List<TenantFeatureDto> =
        systemDbQuery {

            val tenantFeatures = TenantFeatures
                .selectAll()
                .where { TenantFeatures.tenantId eq tenantId }
                .orderBy(TenantFeatures.createdAt to SortOrder.ASC)
                .map { mapRowToDto(it) }

            if (!includeFeature) return@systemDbQuery tenantFeatures

            tenantFeatures.map { tf ->
                tf.copy(
                    feature = tf.featureId?.let {
                        featureRepository.findByIdTx(it)
                    }
                )
            }
        }

    suspend fun findByTenantIdAndFeatureId(
        tenantId: String,
        featureId: Int,
        includeFeature: Boolean = true
    ): TenantFeatureDto? =
        systemDbQuery {

            val tf = TenantFeatures
                .selectAll()
                .where {
                    (TenantFeatures.tenantId eq tenantId) and
                            (TenantFeatures.featureId eq featureId)
                }
                .map { mapRowToDto(it) }
                .singleOrNull()

            if (tf == null || !includeFeature) return@systemDbQuery tf

            tf.copy(
                feature = featureRepository.findByIdTx(featureId)
            )
        }

    // Legacy – kept for backward compatibility
    @Deprecated(
        message = "Use findByTenantIdAndFeatureId instead",
        level = DeprecationLevel.WARNING
    )
    suspend fun findByTenantIdAndFeatureName(
        tenantId: String,
        featureName: String,
        includeFeature: Boolean = true
    ): TenantFeatureDto? =
        systemDbQuery {
            findByTenantIdAndFeatureNameTx(
                tenantId,
                featureName,
                includeFeature
            )
        }

    /* =========================================================
     * UPDATE / DELETE
     * ========================================================= */

    suspend fun update(
        tenantId: String,
        featureId: Int,
        request: UpdateTenantFeatureRequest
    ): Boolean =
        systemDbQuery {

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
                request.customLimitValue?.let { value ->
                    it[customLimitValue] = value
                }
                it[updatedAt] = now
            } > 0
        }

    suspend fun delete(
        tenantId: String,
        featureId: Int
    ): Boolean =
        systemDbQuery {
            TenantFeatures.deleteWhere {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.featureId eq featureId)
            } > 0
        }

    /* =========================================================
     * EXISTS
     * ========================================================= */

    suspend fun exists(
        tenantId: String,
        featureId: Int
    ): Boolean =
        systemDbQuery {
            TenantFeatures
                .selectAll()
                .where {
                    (TenantFeatures.tenantId eq tenantId) and
                            (TenantFeatures.featureId eq featureId)
                }
                .any()
        }

    /* =========================================================
     * ENABLED FEATURES
     * ========================================================= */

    suspend fun getEnabledFeatureKeys(tenantId: String): List<String> =
        systemDbQuery {
            TenantFeatures
                .innerJoin(Features)
                .selectAll()
                .where {
                    (TenantFeatures.tenantId eq tenantId) and
                            (TenantFeatures.isEnabled eq true) and
                            (Features.isActive eq true)
                }
                .map { it[Features.featureKey] }
        }

    suspend fun getEnabledFeatures(tenantId: String): List<TenantFeatureDto> =
        systemDbQuery {

            val tenantFeatures = TenantFeatures
                .selectAll()
                .where {
                    (TenantFeatures.tenantId eq tenantId) and
                            (TenantFeatures.isEnabled eq true)
                }
                .orderBy(TenantFeatures.createdAt to SortOrder.ASC)
                .map { mapRowToDto(it) }

            tenantFeatures.map { tf ->
                tf.copy(
                    feature = tf.featureId?.let {
                        featureRepository.findByIdTx(it)
                    }
                )
            }
        }

    /* =========================================================
     * TRANSACTION-LOCAL HELPERS
     * ========================================================= */

    private fun findByIdTx(
        id: Int,
        includeFeature: Boolean
    ): TenantFeatureDto? {

        val tf = TenantFeatures
            .selectAll()
            .where { TenantFeatures.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()

        if (tf == null || !includeFeature) return tf

        return tf.copy(
            feature = tf.featureId?.let {
                featureRepository.findByIdTx(it)
            }
        )
    }

    private fun findByTenantIdAndFeatureNameTx(
        tenantId: String,
        featureName: String,
        includeFeature: Boolean
    ): TenantFeatureDto? {

        val tf = TenantFeatures
            .selectAll()
            .where {
                (TenantFeatures.tenantId eq tenantId) and
                        (TenantFeatures.featureName eq featureName)
            }
            .map { mapRowToDto(it) }
            .singleOrNull()

        if (tf == null || !includeFeature) return tf

        return tf.copy(
            feature = tf.featureId?.let {
                featureRepository.findByIdTx(it)
            }
        )
    }

    /* =========================================================
     * MAPPER
     * ========================================================= */

    private fun mapRowToDto(row: ResultRow): TenantFeatureDto =
        TenantFeatureDto(
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
            feature = null
        )
}
