package com.example.repositories

import com.example.database.tables.AcademicYears
import com.example.database.tables.Classes
import com.example.database.tables.FeesStructures
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class FeesStructureRepository {

    suspend fun create(request: CreateFeesStructureRequest): String = tenantDbQuery {
        FeesStructures.insert {
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[name] = request.name
            it[amount] = BigDecimal(request.amount)
            it[isMandatory] = request.isMandatory
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[FeesStructures.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateFeesStructureRequest>): List<String> = tenantDbQuery {
        requests.map { request ->
            FeesStructures.insert {
                it[classId] = UUID.fromString(request.classId)
                it[academicYearId] = UUID.fromString(request.academicYearId)
                it[name] = request.name
                it[amount] = BigDecimal(request.amount)
                it[isMandatory] = request.isMandatory
                it[createdAt] = LocalDateTime.now()
                it[updatedAt] = LocalDateTime.now()
            }[FeesStructures.id].toString()
        }
    }

    suspend fun findById(id: String): FeesStructureDto? = tenantDbQuery {
        FeesStructures
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeesStructures.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<FeesStructureDto> = tenantDbQuery {
        FeesStructures
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC, FeesStructures.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateFeesStructureRequest): Boolean = tenantDbQuery {
        FeesStructures.update({ FeesStructures.id eq UUID.fromString(id) }) {
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[name] = request.name
            it[amount] = BigDecimal(request.amount)
            it[isMandatory] = request.isMandatory
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: String): Boolean = tenantDbQuery {
        FeesStructures.deleteWhere { FeesStructures.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByClassId(classId: String): List<FeesStructureDto> = tenantDbQuery {
        FeesStructures
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeesStructures.classId eq UUID.fromString(classId) }
            .orderBy(FeesStructures.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAcademicYear(academicYearId: String): List<FeesStructureDto> = tenantDbQuery {
        FeesStructures
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeesStructures.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC, FeesStructures.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndAcademicYear(classId: String, academicYearId: String): List<FeesStructureDto> = tenantDbQuery {
        FeesStructures
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (FeesStructures.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(FeesStructures.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findMandatoryFees(classId: String, academicYearId: String): List<FeesStructureDto> = tenantDbQuery {
        FeesStructures
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (FeesStructures.academicYearId eq UUID.fromString(academicYearId)) and
                        (FeesStructures.isMandatory eq true)
            }
            .orderBy(FeesStructures.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findOptionalFees(classId: String, academicYearId: String): List<FeesStructureDto> = tenantDbQuery {
        FeesStructures
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (FeesStructures.academicYearId eq UUID.fromString(academicYearId)) and
                        (FeesStructures.isMandatory eq false)
            }
            .orderBy(FeesStructures.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(classId: String, academicYearId: String, name: String, excludeId: String? = null): Boolean = tenantDbQuery {
        val query = FeesStructures.selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (FeesStructures.academicYearId eq UUID.fromString(academicYearId)) and
                        (FeesStructures.name eq name)
            }

        if (excludeId != null) {
            query.andWhere { FeesStructures.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByClassId(classId: String): Int = tenantDbQuery {
        FeesStructures.deleteWhere { FeesStructures.classId eq UUID.fromString(classId) }
    }

    suspend fun deleteByAcademicYear(academicYearId: String): Int = tenantDbQuery {
        FeesStructures.deleteWhere { FeesStructures.academicYearId eq UUID.fromString(academicYearId) }
    }

    suspend fun getClassFeesStructures(academicYearId: String): List<ClassFeesStructureDto> = tenantDbQuery {
        val feeStructures = FeesStructures
            .join(Classes, JoinType.INNER, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeesStructures.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .toList()

        feeStructures.groupBy {
            Triple(
                it[FeesStructures.classId].toString(),
                it[Classes.className],
                it[Classes.sectionName]
            )
        }.map { (classInfo, rows) ->
            val fees = rows.map { mapRowToDto(it) }
            val mandatoryFees = fees.filter { it.isMandatory }.sumOf { BigDecimal(it.amount) }
            val optionalFees = fees.filter { !it.isMandatory }.sumOf { BigDecimal(it.amount) }
            val totalFees = mandatoryFees + optionalFees

            ClassFeesStructureDto(
                classId = classInfo.first,
                className = classInfo.second,
                sectionName = classInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                feeStructures = fees,
                totalMandatoryFees = mandatoryFees.toString(),
                totalOptionalFees = optionalFees.toString(),
                totalFees = totalFees.toString()
            )
        }
    }

    suspend fun getFeesStructureSummary(academicYearId: String): FeesStructureSummaryDto = tenantDbQuery {
        val classSummaries = getClassFeesStructures(academicYearId)
        val totalMandatoryFees = classSummaries.sumOf { BigDecimal(it.totalMandatoryFees) }
        val totalOptionalFees = classSummaries.sumOf { BigDecimal(it.totalOptionalFees) }
        val academicYearName = if (classSummaries.isNotEmpty()) {
            classSummaries.first().academicYearName
        } else {
            AcademicYears.selectAll()
                .where { AcademicYears.id eq UUID.fromString(academicYearId) }
                .singleOrNull()?.get(AcademicYears.year) ?: ""
        }

        FeesStructureSummaryDto(
            academicYearId = academicYearId,
            academicYearName = academicYearName,
            totalClasses = classSummaries.size,
            totalFeeStructures = classSummaries.sumOf { it.feeStructures.size },
            totalMandatoryFees = totalMandatoryFees.toString(),
            totalOptionalFees = totalOptionalFees.toString(),
            classSummaries = classSummaries
        )
    }

    private fun mapRowToDto(row: ResultRow): FeesStructureDto {
        return FeesStructureDto(
            id = row[FeesStructures.id].toString(),
            classId = row[FeesStructures.classId].toString(),
            academicYearId = row[FeesStructures.academicYearId].toString(),
            name = row[FeesStructures.name],
            amount = row[FeesStructures.amount].toString(),
            isMandatory = row[FeesStructures.isMandatory],
            createdAt = row[FeesStructures.createdAt].toString(),
            updatedAt = row[FeesStructures.updatedAt].toString(),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year)
        )
    }
}
