package com.example.repositories

import com.example.database.tables.AcademicYears
import com.example.models.dto.AcademicYearDto
import com.example.models.dto.CreateAcademicYearRequest
import com.example.models.dto.UpdateAcademicYearRequest
import com.example.utils.tenantDbQuery
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.util.*

class AcademicYearRepository {

    suspend fun create(request: CreateAcademicYearRequest): UUID = tenantDbQuery {
        val academicYearId = UUID.randomUUID()
        AcademicYears.insert {
            it[id] = academicYearId
            it[year] = request.year
            it[startDate] = LocalDate.parse(request.startDate)
            it[endDate] = LocalDate.parse(request.endDate)
            it[isActive] = request.isActive
        }
        academicYearId
    }

    suspend fun findById(id: UUID): AcademicYearDto? = tenantDbQuery {
        AcademicYears.selectAll()
            .where { AcademicYears.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<AcademicYearDto> = tenantDbQuery {
        AcademicYears.selectAll()
            .orderBy(AcademicYears.startDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findActiveAcademicYear(): AcademicYearDto? = tenantDbQuery {
        AcademicYears.selectAll()
            .where { AcademicYears.isActive eq true }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByYear(year: String): AcademicYearDto? = tenantDbQuery {
        AcademicYears.selectAll()
            .where { AcademicYears.year eq year }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByDateRange(startDate: String, endDate: String): List<AcademicYearDto> = tenantDbQuery {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        AcademicYears.selectAll()
            .where {
                (AcademicYears.startDate lessEq end) and (AcademicYears.endDate greaterEq start)
            }
            .orderBy(AcademicYears.startDate to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun getCurrentAcademicYear(): AcademicYearDto? = tenantDbQuery {
        val currentDate = LocalDate.now()
        AcademicYears.selectAll()
            .where {
                (AcademicYears.startDate lessEq currentDate) and (AcademicYears.endDate greaterEq currentDate)
            }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun update(id: UUID, request: UpdateAcademicYearRequest): Boolean = tenantDbQuery {
        AcademicYears.update({ AcademicYears.id eq id }) {
            it[year] = request.year
            it[startDate] = LocalDate.parse(request.startDate)
            it[endDate] = LocalDate.parse(request.endDate)
            it[isActive] = request.isActive
        } > 0
    }

    suspend fun setActiveStatus(id: UUID, isActive: Boolean): Boolean = tenantDbQuery {
        AcademicYears.update({ AcademicYears.id eq id }) {
            it[AcademicYears.isActive] = isActive
        } > 0
    }

    suspend fun deactivateAllAcademicYears(): Int = tenantDbQuery {
        AcademicYears.update {
            it[isActive] = false
        }
    }

    suspend fun delete(id: UUID): Boolean = tenantDbQuery {
        AcademicYears.deleteWhere { AcademicYears.id eq id } > 0
    }

    suspend fun yearExists(year: String): Boolean = tenantDbQuery {
        AcademicYears.selectAll()
            .where { AcademicYears.year eq year }
            .count() > 0
    }

    suspend fun yearExistsForOtherAcademicYear(year: String, academicYearId: UUID): Boolean = tenantDbQuery {
        AcademicYears.selectAll()
            .where { (AcademicYears.year eq year) and (AcademicYears.id neq academicYearId) }
            .count() > 0
    }

    suspend fun hasDateOverlap(startDate: String, endDate: String, excludeId: UUID? = null): Boolean = tenantDbQuery {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        var query = AcademicYears.selectAll()
            .where {
                (AcademicYears.startDate lessEq end) and (AcademicYears.endDate greaterEq start)
            }

        if (excludeId != null) {
            query = query.andWhere { AcademicYears.id neq excludeId }
        }

        query.count() > 0
    }

    private fun mapRowToDto(row: ResultRow): AcademicYearDto {
        return AcademicYearDto(
            id = row[AcademicYears.id].toString(),
            year = row[AcademicYears.year],
            startDate = row[AcademicYears.startDate].toString(),
            endDate = row[AcademicYears.endDate].toString(),
            isActive = row[AcademicYears.isActive]
        )
    }
}