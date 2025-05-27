package com.example.repositories

import com.example.database.tables.Holidays
import com.example.models.dto.CreateHolidayRequest
import com.example.models.dto.HolidayDto
import com.example.models.dto.UpdateHolidayRequest
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate

class HolidayRepository {

    suspend fun create(request: CreateHolidayRequest): Int = dbQuery {
        Holidays.insert {
            it[name] = request.name
            it[date] = LocalDate.parse(request.date)
            it[description] = request.description
            it[isPublicHoliday] = request.isPublicHoliday
        }[Holidays.id]
    }

    suspend fun findById(id: Int): HolidayDto? = dbQuery {
        Holidays.selectAll()
            .where { Holidays.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<HolidayDto> = dbQuery {
        Holidays.selectAll()
            .orderBy(Holidays.date to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: Int, request: UpdateHolidayRequest): Boolean = dbQuery {
        Holidays.update({ Holidays.id eq id }) {
            it[name] = request.name
            it[date] = LocalDate.parse(request.date)
            it[description] = request.description
            it[isPublicHoliday] = request.isPublicHoliday
        } > 0
    }

    suspend fun delete(id: Int): Boolean = dbQuery {
        Holidays.deleteWhere { Holidays.id eq id } > 0
    }

    suspend fun findByDateRange(startDate: String, endDate: String): List<HolidayDto> = dbQuery {
        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        Holidays.selectAll()
            .where { (Holidays.date greaterEq start) and (Holidays.date lessEq end) }
            .orderBy(Holidays.date to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findPublicHolidays(): List<HolidayDto> = dbQuery {
        Holidays.selectAll()
            .where { Holidays.isPublicHoliday eq true }
            .orderBy(Holidays.date to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    private fun mapRowToDto(row: ResultRow): HolidayDto {
        return HolidayDto(
            id = row[Holidays.id],
            name = row[Holidays.name],
            date = row[Holidays.date].toString(),
            description = row[Holidays.description],
            isPublicHoliday = row[Holidays.isPublicHoliday]
        )
    }
}