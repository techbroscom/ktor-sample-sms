package com.example.repositories

import com.example.database.tables.TenantConfig
import com.example.models.dto.CreateTenantConfigRequest
import com.example.models.dto.TenantConfigDto
import com.example.models.dto.UpdateTenantConfigRequest
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TenantConfigRepository {

    private val dateFormatter = DateTimeFormatter.ISO_DATE_TIME

    suspend fun create(request: CreateTenantConfigRequest): TenantConfigDto = dbQuery {
        val now = LocalDateTime.now()
        val insertedId = TenantConfig.insert {
            it[tenantId] = request.tenantId
            it[schemaName] = request.schemaName
            it[tenantName] = request.tenantName
            it[subscriptionStatus] = request.subscriptionStatus
            it[isPaid] = request.isPaid
            it[subscriptionStartDate] = now
            it[subscriptionEndDate] = null
            it[planType] = request.planType
            it[storageAllocatedMB] = request.storageAllocatedMB
            it[storageUsedMB] = 0
            it[maxUsers] = request.maxUsers
            it[maxStudents] = request.maxStudents
            it[createdAt] = now
            it[updatedAt] = now
            it[isActive] = true
            it[notes] = request.notes
        } get TenantConfig.id

        findById(insertedId)!!
    }

    suspend fun findById(id: Int): TenantConfigDto? = dbQuery {
        TenantConfig.selectAll()
            .where { TenantConfig.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByTenantId(tenantId: String): TenantConfigDto? = dbQuery {
        TenantConfig.selectAll()
            .where { TenantConfig.tenantId eq tenantId }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(activeOnly: Boolean = false): List<TenantConfigDto> = dbQuery {
        val query = if (activeOnly) {
            TenantConfig.selectAll().where { TenantConfig.isActive eq true }
        } else {
            TenantConfig.selectAll()
        }
        query.orderBy(TenantConfig.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(tenantId: String, request: UpdateTenantConfigRequest): Boolean = dbQuery {
        val now = LocalDateTime.now()
        TenantConfig.update({ TenantConfig.tenantId eq tenantId }) {
            request.tenantName?.let { name -> it[tenantName] = name }
            request.subscriptionStatus?.let { status -> it[subscriptionStatus] = status }
            request.isPaid?.let { paid -> it[isPaid] = paid }
            request.subscriptionEndDate?.let { date ->
                it[subscriptionEndDate] = LocalDateTime.parse(date, dateFormatter)
            }
            request.planType?.let { plan -> it[planType] = plan }
            request.storageAllocatedMB?.let { storage -> it[storageAllocatedMB] = storage }
            request.storageUsedMB?.let { storage -> it[storageUsedMB] = storage }
            request.maxUsers?.let { max -> it[maxUsers] = max }
            request.maxStudents?.let { max -> it[maxStudents] = max }
            request.isActive?.let { active -> it[isActive] = active }
            request.notes?.let { note -> it[notes] = note }
            it[updatedAt] = now
        } > 0
    }

    suspend fun delete(tenantId: String): Boolean = dbQuery {
        // Soft delete - mark as inactive
        TenantConfig.update({ TenantConfig.tenantId eq tenantId }) {
            it[isActive] = false
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun exists(tenantId: String): Boolean = dbQuery {
        TenantConfig.selectAll()
            .where { TenantConfig.tenantId eq tenantId }
            .any()
    }

    private fun mapRowToDto(row: ResultRow): TenantConfigDto {
        return TenantConfigDto(
            id = row[TenantConfig.id],
            tenantId = row[TenantConfig.tenantId],
            schemaName = row[TenantConfig.schemaName],
            tenantName = row[TenantConfig.tenantName],
            subscriptionStatus = row[TenantConfig.subscriptionStatus],
            isPaid = row[TenantConfig.isPaid],
            subscriptionStartDate = row[TenantConfig.subscriptionStartDate].format(dateFormatter),
            subscriptionEndDate = row[TenantConfig.subscriptionEndDate]?.format(dateFormatter),
            planType = row[TenantConfig.planType],
            storageAllocatedMB = row[TenantConfig.storageAllocatedMB],
            storageUsedMB = row[TenantConfig.storageUsedMB],
            maxUsers = row[TenantConfig.maxUsers],
            maxStudents = row[TenantConfig.maxStudents],
            createdAt = row[TenantConfig.createdAt].format(dateFormatter),
            updatedAt = row[TenantConfig.updatedAt].format(dateFormatter),
            isActive = row[TenantConfig.isActive],
            notes = row[TenantConfig.notes],
            features = null // Will be populated by service layer
        )
    }
}
