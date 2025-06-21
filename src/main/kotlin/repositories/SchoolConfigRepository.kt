package com.example.repositories

import com.example.database.tables.SchoolConfig
import com.example.models.dto.SchoolConfigDto
import com.example.models.dto.UpdateSchoolConfigRequest
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class SchoolConfigRepository {

    suspend fun insertDefaultIfNotExists() = dbQuery {
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

    suspend fun findById(id: Int): SchoolConfigDto? = dbQuery {
        SchoolConfig.selectAll()
            .where { SchoolConfig.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<SchoolConfigDto> = dbQuery {
        SchoolConfig.selectAll()
            .orderBy(SchoolConfig.id to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: Int, request: UpdateSchoolConfigRequest): Boolean = dbQuery {
        SchoolConfig.update({ SchoolConfig.id eq id }) {
            it[schoolName] = request.schoolName
            it[address] = request.address
            it[logoUrl] = request.logoUrl
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
        return SchoolConfigDto(
            id = row[SchoolConfig.id],
            schoolName = row[SchoolConfig.schoolName],
            address = row[SchoolConfig.address],
            logoUrl = row[SchoolConfig.logoUrl],
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