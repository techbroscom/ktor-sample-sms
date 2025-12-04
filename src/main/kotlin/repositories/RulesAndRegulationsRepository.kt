package com.example.repositories

import com.example.database.tables.Rules
import com.example.models.dto.CreateRulesAndRegulationsRequest
import com.example.models.dto.RulesAndRegulationsDto
import com.example.models.dto.UpdateRulesAndRegulationsRequest
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class RulesAndRegulationsRepository {

    suspend fun create(request: CreateRulesAndRegulationsRequest): Int = tenantDbQuery {
        val now = LocalDateTime.now()
        Rules.insert {
            it[rule] = request.rule
            it[createdAt] = now
            it[updatedAt] = now
        }[Rules.id]
    }

    suspend fun findById(id: Int): RulesAndRegulationsDto? = tenantDbQuery {
        Rules.selectAll()
            .where { Rules.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<RulesAndRegulationsDto> = tenantDbQuery {
        Rules.selectAll()
            .orderBy(Rules.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: Int, request: UpdateRulesAndRegulationsRequest): Boolean = tenantDbQuery {
        Rules.update({ Rules.id eq id }) {
            it[rule] = request.rule
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: Int): Boolean = tenantDbQuery {
        Rules.deleteWhere { Rules.id eq id } > 0
    }

    suspend fun searchByKeyword(keyword: String): List<RulesAndRegulationsDto> = tenantDbQuery {
        Rules.selectAll()
            .where { Rules.rule like "%$keyword%" }
            .orderBy(Rules.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findRecent(limit: Int = 10): List<RulesAndRegulationsDto> = tenantDbQuery {
        Rules.selectAll()
            .orderBy(Rules.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { mapRowToDto(it) }
    }

    suspend fun findByDateRange(startDate: String, endDate: String): List<RulesAndRegulationsDto> = tenantDbQuery {
        val start = LocalDateTime.parse("${startDate}T00:00:00")
        val end = LocalDateTime.parse("${endDate}T23:59:59")

        Rules.selectAll()
            .where { (Rules.createdAt greaterEq start) and (Rules.createdAt lessEq end) }
            .orderBy(Rules.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun getTotalCount(): Long = tenantDbQuery {
        Rules.selectAll().count()
    }

    private fun mapRowToDto(row: ResultRow): RulesAndRegulationsDto {
        return RulesAndRegulationsDto(
            id = row[Rules.id],
            rule = row[Rules.rule],
            createdAt = row[Rules.createdAt].toString(),
            updatedAt = row[Rules.updatedAt].toString()
        )
    }
}