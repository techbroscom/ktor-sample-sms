package com.example.repositories

import com.example.database.tables.Features
import com.example.models.dto.CreateFeatureRequest
import com.example.models.dto.FeatureDto
import com.example.models.dto.UpdateFeatureRequest
import com.example.utils.systemDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FeatureRepository {

    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    suspend fun create(request: CreateFeatureRequest): FeatureDto = systemDbQuery {
        val now = LocalDateTime.now()
        val insertedId = Features.insert {
            it[featureKey] = request.featureKey
            it[name] = request.name
            it[description] = request.description
            it[category] = request.category
            it[isActive] = request.isActive
            it[defaultEnabled] = request.defaultEnabled
            it[hasLimit] = request.hasLimit
            it[limitType] = request.limitType
            it[limitValue] = request.limitValue
            it[limitUnit] = request.limitUnit
            it[createdAt] = now
            it[updatedAt] = null
        } get Features.id

        findById(insertedId)!!
    }

    suspend fun findById(id: Int): FeatureDto? = systemDbQuery {
        Features.selectAll()
            .where { Features.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByFeatureKey(featureKey: String): FeatureDto? = systemDbQuery {
        Features.selectAll()
            .where { Features.featureKey eq featureKey }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(activeOnly: Boolean = false): List<FeatureDto> = systemDbQuery {
        val query = if (activeOnly) {
            Features.selectAll().where { Features.isActive eq true }
        } else {
            Features.selectAll()
        }
        query.orderBy(Features.category to SortOrder.ASC, Features.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByCategory(category: String): List<FeatureDto> = systemDbQuery {
        Features.selectAll()
            .where { Features.category eq category }
            .orderBy(Features.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: Int, request: UpdateFeatureRequest): Boolean = systemDbQuery {
        Features.update({ Features.id eq id }) {
            request.name?.let { value -> it[name] = value }
            request.description?.let { value -> it[description] = value }
            request.category?.let { value -> it[category] = value }
            request.isActive?.let { value -> it[isActive] = value }
            request.defaultEnabled?.let { value -> it[defaultEnabled] = value }
            request.hasLimit?.let { value -> it[hasLimit] = value }
            request.limitType?.let { value -> it[limitType] = value }
            request.limitValue?.let { value -> it[limitValue] = value }
            request.limitUnit?.let { value -> it[limitUnit] = value }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: Int): Boolean = systemDbQuery {
        Features.deleteWhere { Features.id eq id } > 0
    }

    suspend fun exists(featureKey: String): Boolean = systemDbQuery {
        Features.selectAll()
            .where { Features.featureKey eq featureKey }
            .any()
    }

    suspend fun existsById(id: Int): Boolean = systemDbQuery {
        Features.selectAll()
            .where { Features.id eq id }
            .any()
    }

    private fun mapRowToDto(row: ResultRow): FeatureDto {
        return FeatureDto(
            id = row[Features.id],
            featureKey = row[Features.featureKey],
            name = row[Features.name],
            description = row[Features.description],
            category = row[Features.category],
            isActive = row[Features.isActive],
            defaultEnabled = row[Features.defaultEnabled],
            hasLimit = row[Features.hasLimit],
            limitType = row[Features.limitType],
            limitValue = row[Features.limitValue],
            limitUnit = row[Features.limitUnit],
            createdAt = row[Features.createdAt].format(dateFormatter),
            updatedAt = row[Features.updatedAt]?.format(dateFormatter)
        )
    }
}
