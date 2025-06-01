package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.AcademicYearRepository
import io.ktor.http.*
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

class AcademicYearService(private val academicYearRepository: AcademicYearRepository) {

    suspend fun createAcademicYear(request: CreateAcademicYearRequest): AcademicYearDto {
        validateAcademicYearRequest(request.year, request.startDate, request.endDate)

        // Check if year already exists
        if (academicYearRepository.yearExists(request.year)) {
            throw ApiException("Academic year already exists", HttpStatusCode.Conflict)
        }

        // Check for date overlaps
        if (academicYearRepository.hasDateOverlap(request.startDate, request.endDate)) {
            throw ApiException("Academic year dates overlap with existing academic year", HttpStatusCode.Conflict)
        }

        // If setting as active, deactivate all other academic years
        if (request.isActive) {
            academicYearRepository.deactivateAllAcademicYears()
        }

        val academicYearId = academicYearRepository.create(request)
        return getAcademicYearById(academicYearId)
    }

    suspend fun getAcademicYearById(id: UUID): AcademicYearDto {
        return academicYearRepository.findById(id)
            ?: throw ApiException("Academic year not found", HttpStatusCode.NotFound)
    }

    suspend fun getAcademicYearById(id: String): AcademicYearDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid academic year ID format", HttpStatusCode.BadRequest)
        }
        return getAcademicYearById(uuid)
    }

    suspend fun getAllAcademicYears(): List<AcademicYearDto> {
        return academicYearRepository.findAll()
    }

    suspend fun getActiveAcademicYear(): AcademicYearDto {
        return academicYearRepository.findActiveAcademicYear()
            ?: throw ApiException("No active academic year found", HttpStatusCode.NotFound)
    }

    suspend fun getCurrentAcademicYear(): AcademicYearDto {
        return academicYearRepository.getCurrentAcademicYear()
            ?: throw ApiException("No current academic year found", HttpStatusCode.NotFound)
    }

    suspend fun getAcademicYearByYear(year: String): AcademicYearDto {
        validateYearFormat(year)
        return academicYearRepository.findByYear(year)
            ?: throw ApiException("Academic year not found", HttpStatusCode.NotFound)
    }

    suspend fun getAcademicYearsByDateRange(startDate: String, endDate: String): List<AcademicYearDto> {
        validateDateFormat(startDate, "Start date")
        validateDateFormat(endDate, "End date")

        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        if (start.isAfter(end)) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        return academicYearRepository.findByDateRange(startDate, endDate)
    }

    suspend fun updateAcademicYear(id: String, request: UpdateAcademicYearRequest): AcademicYearDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid academic year ID format", HttpStatusCode.BadRequest)
        }

        validateAcademicYearRequest(request.year, request.startDate, request.endDate)

        // Check if year already exists for other academic years
        if (academicYearRepository.yearExistsForOtherAcademicYear(request.year, uuid)) {
            throw ApiException("Academic year already exists", HttpStatusCode.Conflict)
        }

        // Check for date overlaps (excluding current academic year)
        if (academicYearRepository.hasDateOverlap(request.startDate, request.endDate, uuid)) {
            throw ApiException("Academic year dates overlap with existing academic year", HttpStatusCode.Conflict)
        }

        // If setting as active, deactivate all other academic years
        if (request.isActive) {
            academicYearRepository.deactivateAllAcademicYears()
        }

        val updated = academicYearRepository.update(uuid, request)
        if (!updated) {
            throw ApiException("Academic year not found", HttpStatusCode.NotFound)
        }

        return getAcademicYearById(uuid)
    }

    suspend fun setActiveAcademicYear(id: String, request: SetActiveAcademicYearRequest): AcademicYearDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid academic year ID format", HttpStatusCode.BadRequest)
        }

        // If setting as active, deactivate all other academic years
        if (request.isActive) {
            academicYearRepository.deactivateAllAcademicYears()
        }

        val updated = academicYearRepository.setActiveStatus(uuid, request.isActive)
        if (!updated) {
            throw ApiException("Academic year not found", HttpStatusCode.NotFound)
        }

        return getAcademicYearById(uuid)
    }

    suspend fun deleteAcademicYear(id: String) {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid academic year ID format", HttpStatusCode.BadRequest)
        }

        val deleted = academicYearRepository.delete(uuid)
        if (!deleted) {
            throw ApiException("Academic year not found", HttpStatusCode.NotFound)
        }
    }

    private fun validateAcademicYearRequest(year: String, startDate: String, endDate: String) {
        validateYearFormat(year)
        validateDateFormat(startDate, "Start date")
        validateDateFormat(endDate, "End date")

        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        if (start.isAfter(end)) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        // Validate that the year format matches the actual dates
        val startYear = start.year
        val endYear = end.year
        val expectedYear = if (start.monthValue >= 6) { // Assuming academic year starts around June
            "$startYear-${startYear + 1}"
        } else {
            "${startYear - 1}-$startYear"
        }

        // This is a flexible check - you might want to adjust based on your business rules
        if (!year.matches(Regex("\\d{4}-\\d{4}"))) {
            throw ApiException("Year must be in format YYYY-YYYY (e.g., 2024-2025)", HttpStatusCode.BadRequest)
        }
    }

    private fun validateYearFormat(year: String) {
        when {
            year.isBlank() -> throw ApiException("Academic year cannot be empty", HttpStatusCode.BadRequest)
            year.length > 9 -> throw ApiException("Academic year is too long (max 9 characters)", HttpStatusCode.BadRequest)
            !year.matches(Regex("\\d{4}-\\d{4}")) -> throw ApiException("Year must be in format YYYY-YYYY (e.g., 2024-2025)", HttpStatusCode.BadRequest)
        }
    }

    private fun validateDateFormat(date: String, fieldName: String) {
        try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw ApiException("$fieldName must be in format YYYY-MM-DD", HttpStatusCode.BadRequest)
        }
    }
}