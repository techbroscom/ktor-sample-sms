package com.example.repositories

import com.example.database.tables.Features
import com.example.database.tables.UserPermissions
import com.example.models.dto.AssignUserPermissionsRequest
import com.example.models.dto.UpdateUserPermissionRequest
import com.example.models.dto.UserPermissionDto
import com.example.utils.systemDbQuery
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
        tenantId: String,
        featureIds: List<Int>,
        grantedBy: UUID?
    ): List<UserPermissionDto> = systemDbQuery {
        val now = LocalDateTime.now()
        val results = mutableListOf<UserPermissionDto>()

        featureIds.forEach { featureId ->
            // Check if permission already exists
            val existing = findByUserAndFeature(userId, tenantId, featureId)
            if (existing == null) {
                val insertedId = UserPermissions.insert {
                    it[UserPermissions.userId] = userId
                    it[UserPermissions.tenantId] = tenantId
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

    suspend fun findById(id: Int, includeFeature: Boolean = true): UserPermissionDto? = systemDbQuery {
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

    suspend fun findByUser(userId: UUID, tenantId: String, includeFeature: Boolean = true): List<UserPermissionDto> = systemDbQuery {
        val results = UserPermissions.selectAll()
            .where {
                (UserPermissions.userId eq userId) and
                        (UserPermissions.tenantId eq tenantId)
            }
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

    suspend fun findByUserAndFeature(userId: UUID, tenantId: String, featureId: Int): UserPermissionDto? = systemDbQuery {
        UserPermissions.selectAll()
            .where {
                (UserPermissions.userId eq userId) and
                        (UserPermissions.tenantId eq tenantId) and
                        (UserPermissions.featureId eq featureId)
            }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun update(userId: UUID, tenantId: String, featureId: Int, request: UpdateUserPermissionRequest): Boolean = systemDbQuery {
        UserPermissions.update({
            (UserPermissions.userId eq userId) and
                    (UserPermissions.tenantId eq tenantId) and
                    (UserPermissions.featureId eq featureId)
        }) {
            it[isEnabled] = request.isEnabled
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(userId: UUID, tenantId: String, featureId: Int): Boolean = systemDbQuery {
        UserPermissions.deleteWhere {
            (UserPermissions.userId eq userId) and
                    (UserPermissions.tenantId eq tenantId) and
                    (UserPermissions.featureId eq featureId)
        } > 0
    }

    suspend fun deleteAllForUser(userId: UUID, tenantId: String): Boolean = systemDbQuery {
        UserPermissions.deleteWhere {
            (UserPermissions.userId eq userId) and
                    (UserPermissions.tenantId eq tenantId)
        } > 0
    }

    suspend fun getEnabledFeatureKeys(userId: UUID, tenantId: String): List<String> = systemDbQuery {
        UserPermissions.innerJoin(Features, { featureId }, { Features.id })
            .selectAll()
            .where {
                (UserPermissions.userId eq userId) and
                        (UserPermissions.tenantId eq tenantId) and
                        (UserPermissions.isEnabled eq true) and
                        (Features.isActive eq true)
            }
            .map { it[Features.featureKey] }
    }

    suspend fun exists(userId: UUID, tenantId: String, featureId: Int): Boolean = systemDbQuery {
        UserPermissions.selectAll()
            .where {
                (UserPermissions.userId eq userId) and
                        (UserPermissions.tenantId eq tenantId) and
                        (UserPermissions.featureId eq featureId)
            }
            .any()
    }

    private fun mapRowToDto(row: ResultRow): UserPermissionDto {
        return UserPermissionDto(
            id = row[UserPermissions.id],
            userId = row[UserPermissions.userId].toString(),
            tenantId = row[UserPermissions.tenantId],
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
