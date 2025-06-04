package com.example.repositories

import com.example.database.tables.Subjects
import com.example.models.dto.CreateSubjectRequest
import com.example.models.dto.SubjectDto
import com.example.models.dto.UpdateSubjectRequest
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class SubjectRepository {

    suspend fun create(request: CreateSubjectRequest): String = dbQuery {
        Subjects.insert {
            it[name] = request.name
            it[code] = request.code
        }[Subjects.id].toString()
    }

    suspend fun findById(id: String): SubjectDto? = dbQuery {
        Subjects.selectAll()
            .where { Subjects.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<SubjectDto> = dbQuery {
        Subjects.selectAll()
            .orderBy(Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateSubjectRequest): Boolean = dbQuery {
        Subjects.update({ Subjects.id eq UUID.fromString(id) }) {
            it[name] = request.name
            it[code] = request.code
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        Subjects.deleteWhere { Subjects.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByCode(code: String): SubjectDto? = dbQuery {
        Subjects.selectAll()
            .where { Subjects.code eq code }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByName(name: String): List<SubjectDto> = dbQuery {
        Subjects.selectAll()
            .where { Subjects.name.lowerCase() like "%${name.lowercase()}%" }
            .orderBy(Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicateCode(code: String, excludeId: String? = null): Boolean = dbQuery {
        val query = Subjects.selectAll()
            .where { Subjects.code eq code }

        if (excludeId != null) {
            query.andWhere { Subjects.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun checkDuplicateName(name: String, excludeId: String? = null): Boolean = dbQuery {
        val query = Subjects.selectAll()
            .where { Subjects.name.lowerCase() eq name.lowercase() }

        if (excludeId != null) {
            query.andWhere { Subjects.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun getTotalCount(): Long = dbQuery {
        Subjects.selectAll().count()
    }

    suspend fun findPaginated(offset: Long, limit: Int): List<SubjectDto> = dbQuery {
        Subjects.selectAll()
            .orderBy(Subjects.name to SortOrder.ASC)
            .limit(limit).offset(offset)
            .map { mapRowToDto(it) }
    }

    private fun mapRowToDto(row: ResultRow): SubjectDto {
        return SubjectDto(
            id = row[Subjects.id].toString(),
            name = row[Subjects.name],
            code = row[Subjects.code]
        )
    }
}