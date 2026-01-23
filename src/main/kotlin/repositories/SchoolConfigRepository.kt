package com.example.repositories

import com.example.database.tables.SchoolConfig
import com.example.models.dto.SchoolConfigDto
import com.example.models.dto.UpdateSchoolConfigRequest
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import services.S3FileService

class SchoolConfigRepository(
    private val s3FileService: S3FileService? = null
) {

    suspend fun insertDefaultIfNotExists() = tenantDbQuery {
        val exists = SchoolConfig
            .selectAll()
            .where { SchoolConfig.id eq 1 }
            .any()
        if (!exists) {
            SchoolConfig.insert {
                it[id] = 1
                it[schoolName] = "My School"
                it[address] = "My Address"
                it[logoUrl] = null
                it[email] = null
                it[phoneNumber1] = null
                it[phoneNumber2] = null
                it[phoneNumber3] = null
                it[phoneNumber4] = null
                it[phoneNumber5] = null
                it[website] = null
            }
        }
    }

    suspend fun findById(id: Int): SchoolConfigDto? = tenantDbQuery {
        SchoolConfig.selectAll()
            .where { SchoolConfig.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<SchoolConfigDto> = tenantDbQuery {
        SchoolConfig.selectAll()
            .orderBy(SchoolConfig.id to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: Int, request: UpdateSchoolConfigRequest): Boolean = tenantDbQuery {
        SchoolConfig.update({ SchoolConfig.id eq id }) {
            it[schoolName] = request.schoolName
            it[address] = request.address
            it[logoUrl] = request.logoUrl
            it[logoS3Key] = request.logoS3Key
            it[email] = request.email
            it[phoneNumber1] = request.phoneNumber1
            it[phoneNumber2] = request.phoneNumber2
            it[phoneNumber3] = request.phoneNumber3
            it[phoneNumber4] = request.phoneNumber4
            it[phoneNumber5] = request.phoneNumber5
            it[website] = request.website
        } > 0
    }

    private fun mapRowToDto(row: ResultRow): SchoolConfigDto {
        val logoS3Key = row[SchoolConfig.logoS3Key]
        // Generate public URL from S3 key - no signing, no expiration
        // For public content (school logos), use public URLs instead of signed URLs
        val logoUrl = if (!logoS3Key.isNullOrBlank()) {
            s3FileService?.generatePublicUrlByKey(logoS3Key) ?: row[SchoolConfig.logoUrl]
        } else {
            row[SchoolConfig.logoUrl]
        }

        return SchoolConfigDto(
            id = row[SchoolConfig.id],
            schoolName = row[SchoolConfig.schoolName],
            address = row[SchoolConfig.address],
            logoUrl = logoUrl,
            logoS3Key = logoS3Key,
            email = row[SchoolConfig.email],
            phoneNumber1 = row[SchoolConfig.phoneNumber1],
            phoneNumber2 = row[SchoolConfig.phoneNumber2],
            phoneNumber3 = row[SchoolConfig.phoneNumber3],
            phoneNumber4 = row[SchoolConfig.phoneNumber4],
            phoneNumber5 = row[SchoolConfig.phoneNumber5],
            website = row[SchoolConfig.website]
        )
    }
}