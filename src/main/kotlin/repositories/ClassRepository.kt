package com.example.repositories

import com.example.database.tables.AcademicYears
import com.example.database.tables.Classes
import com.example.models.dto.ClassDto
import com.example.models.dto.CreateClassRequest
import com.example.models.dto.UpdateClassRequest
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ClassRepository {

    suspend fun create(request: CreateClassRequest): String = dbQuery {
        Classes.insert {
            it[className] = request.className
            it[sectionName] = request.sectionName
            it[academicYearId] = UUID.fromString(request.academicYearId)
        }[Classes.id].toString()
    }

    suspend fun findById(id: String): ClassDto? = dbQuery {
        Classes.join(AcademicYears, JoinType.LEFT, Classes.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Classes.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<ClassDto> = dbQuery {
        Classes.join(AcademicYears, JoinType.LEFT, Classes.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateClassRequest): Boolean = dbQuery {
        Classes.update({ Classes.id eq UUID.fromString(id) }) {
            it[className] = request.className
            it[sectionName] = request.sectionName
            it[academicYearId] = UUID.fromString(request.academicYearId)
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        Classes.deleteWhere { Classes.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByAcademicYear(academicYearId: String): List<ClassDto> = dbQuery {
        Classes.join(AcademicYears, JoinType.LEFT, Classes.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Classes.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassNameAndSection(className: String, sectionName: String): List<ClassDto> = dbQuery {
        Classes.join(AcademicYears, JoinType.LEFT, Classes.academicYearId, AcademicYears.id)
            .selectAll()
            .where { (Classes.className eq className) and (Classes.sectionName eq sectionName) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicateClass(className: String, sectionName: String, academicYearId: String, excludeId: String? = null): Boolean = dbQuery {
        val query = Classes.selectAll()
            .where {
                (Classes.className eq className) and
                        (Classes.sectionName eq sectionName) and
                        (Classes.academicYearId eq UUID.fromString(academicYearId))
            }

        if (excludeId != null) {
            query.andWhere { Classes.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    private fun mapRowToDto(row: ResultRow): ClassDto {
        return ClassDto(
            id = row[Classes.id].toString(),
            className = row[Classes.className],
            sectionName = row[Classes.sectionName],
            academicYearId = row[Classes.academicYearId].toString(),
            academicYearName = row.getOrNull(AcademicYears.year) // Assuming AcademicYears has yearName field
        )
    }
}