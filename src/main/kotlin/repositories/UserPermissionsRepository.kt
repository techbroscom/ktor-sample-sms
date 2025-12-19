package com.example.repositories

import com.example.database.tables.Features
import com.example.database.tables.UserPermissions
import com.example.models.dto.AssignUserPermissionsRequest
import com.example.models.dto.UpdateUserPermissionRequest
import com.example.models.dto.UserPermissionDto
import com.example.utils.systemDbQuery
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class UserPermissionsRepository(
    private val featureRepository: FeatureRepository
) {

    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    suspend fun assignPermissions(
        userId: UUID,
        featureIds: List<Int>,
        grantedBy: UUID?
    ): List<UserPermissionDto> = tenantDbQuery {
        val now = LocalDateTime.now()
        val results = mutableListOf<UserPermissionDto>()

        featureIds.forEach { featureId ->
            // Check if permission already exists
            val existing = findByUserAndFeature(userId, featureId)
            if (existing == null) {
                val insertedId = UserPermissions.insert {
                    it[UserPermissions.userId] = userId
                    it[UserPermissions.featureId] = featureId
                    it[isEnabled] = true
                    it[grantedAt] = now
                    it[UserPermissions.grantedBy] = grantedBy
                    it[createdAt] = now
                    it[updatedAt] = null
                } get UserPermissions.id

                findById(insertedId)?.let { results.add(it) }
            } else {
                results.add(existing)
            }
        }

        results
    }

    suspend fun findById(id: Int, includeFeature: Boolean = true): UserPermissionDto? = tenantDbQuery {
        val result = UserPermissions.selectAll()
            .where { UserPermissions.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()

        if (result != null && includeFeature) {
            val feature = featureRepository.findById(result.featureId)
            result.copy(feature = feature)
        } else {
            result
        }
    }

    suspend fun findByUser(userId: UUID, includeFeature: Boolean = true): List<UserPermissionDto> = tenantDbQuery {
        val results = UserPermissions.selectAll()
            .where { UserPermissions.userId eq userId }
            .orderBy(UserPermissions.createdAt to SortOrder.ASC)
            .map { mapRowToDto(it) }

        if (includeFeature) {
            results.map { result ->
                val feature = featureRepository.findById(result.featureId)
                result.copy(feature = feature)
            }
        } else {
            results
        }
    }

    suspend fun findByUserAndFeature(userId: UUID, featureId: Int): UserPermissionDto? = tenantDbQuery {
        UserPermissions.selectAll()
            .where {
                (UserPermissions.userId eq userId) and
                        (UserPermissions.featureId eq featureId)
            }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun update(userId: UUID, featureId: Int, request: UpdateUserPermissionRequest): Boolean = tenantDbQuery {
        UserPermissions.update({
            (UserPermissions.userId eq userId) and
                    (UserPermissions.featureId eq featureId)
        }) {
            it[isEnabled] = request.isEnabled
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(userId: UUID, featureId: Int): Boolean = tenantDbQuery {
        UserPermissions.deleteWhere {
            (UserPermissions.userId eq userId) and
                    (UserPermissions.featureId eq featureId)
        } > 0
    }

    suspend fun deleteAllForUser(userId: UUID): Boolean = tenantDbQuery {
        UserPermissions.deleteWhere {
            UserPermissions.userId eq userId
        } > 0
    }

    suspend fun getEnabledFeatureKeys(userId: UUID): List<String> = tenantDbQuery {
        // Note: Can't do cross-schema join in single query, so we get feature IDs first
        val featureIds = UserPermissions.selectAll()
            .where {
                (UserPermissions.userId eq userId) and
                        (UserPermissions.isEnabled eq true)
            }
            .map { it[UserPermissions.featureId] }

        // Then query Features table in system DB
        if (featureIds.isEmpty()) {
            emptyList()
        } else {
            systemDbQuery {
                Features.selectAll()
                    .where {
                        (Features.id inList featureIds) and
                                (Features.isActive eq true)
                    }
                    .map { it[Features.featureKey] }
            }
        }
    }

    suspend fun exists(userId: UUID, featureId: Int): Boolean = tenantDbQuery {
        UserPermissions.selectAll()
            .where {
                (UserPermissions.userId eq userId) and
                        (UserPermissions.featureId eq featureId)
            }
            .any()
    }

    private fun mapRowToDto(row: ResultRow): UserPermissionDto {
        return UserPermissionDto(
            id = row[UserPermissions.id],
            userId = row[UserPermissions.userId].toString(),
            featureId = row[UserPermissions.featureId],
            isEnabled = row[UserPermissions.isEnabled],
            grantedAt = row[UserPermissions.grantedAt].format(dateFormatter),
            grantedBy = row[UserPermissions.grantedBy]?.toString(),
            createdAt = row[UserPermissions.createdAt].format(dateFormatter),
            updatedAt = row[UserPermissions.updatedAt]?.format(dateFormatter),
            feature = null // Will be populated by caller if needed
        )
    }
}
