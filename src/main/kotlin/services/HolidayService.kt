package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreateHolidayRequest
import com.example.models.dto.HolidayDto
import com.example.models.dto.UpdateHolidayRequest
import com.example.repositories.HolidayRepository
import io.ktor.http.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

class HolidayService(private val holidayRepository: HolidayRepository) {

    suspend fun createHoliday(request: CreateHolidayRequest): HolidayDto {
        validateHolidayRequest(request.name, request.date)

        val holidayId = holidayRepository.create(request)
        return getHolidayById(holidayId)
    }

    suspend fun getHolidayById(id: Int): HolidayDto {
        return holidayRepository.findById(id)
            ?: throw ApiException("Holiday not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllHolidays(): List<HolidayDto> {
        return holidayRepository.findAll()
    }

    suspend fun updateHoliday(id: Int, request: UpdateHolidayRequest): HolidayDto {
        validateHolidayRequest(request.name, request.date)

        val updated = holidayRepository.update(id, request)
        if (!updated) {
            throw ApiException("Holiday not found", HttpStatusCode.NotFound)
        }

        return getHolidayById(id)
    }

    suspend fun deleteHoliday(id: Int) {
        val deleted = holidayRepository.delete(id)
        if (!deleted) {
            throw ApiException("Holiday not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getHolidaysByDateRange(startDate: String, endDate: String): List<HolidayDto> {
        validateDateFormat(startDate, "Start date")
        validateDateFormat(endDate, "End date")

        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        if (start.isAfter(end)) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        return holidayRepository.findByDateRange(startDate, endDate)
    }

    suspend fun getPublicHolidays(): List<HolidayDto> {
        return holidayRepository.findPublicHolidays()
    }

    private fun validateHolidayRequest(name: String, date: String) {
        when {
            name.isBlank() -> throw ApiException("Holiday name cannot be empty", HttpStatusCode.BadRequest)
            name.length > 255 -> throw ApiException("Holiday name is too long (max 255 characters)", HttpStatusCode.BadRequest)
        }

        validateDateFormat(date, "Date")
    }

    private fun validateDateFormat(date: String, fieldName: String) {
        try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw ApiException("$fieldName must be in format YYYY-MM-DD", HttpStatusCode.BadRequest)
        }
    }
}