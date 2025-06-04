package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.ClassDto
import com.example.models.dto.CreateClassRequest
import com.example.models.dto.UpdateClassRequest
import com.example.repositories.ClassRepository
import com.example.services.AcademicYearService
import io.ktor.http.*

class ClassService(
    private val classRepository: ClassRepository,
    private val academicYearService: AcademicYearService
) {

    suspend fun createClass(request: CreateClassRequest): ClassDto {
        validateClassRequest(request.className, request.sectionName, request.academicYearId)

        // Check if academic year exists
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate class in the same academic year
        val isDuplicate = classRepository.checkDuplicateClass(
            request.className,
            request.sectionName,
            request.academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "Class ${request.className} Section ${request.sectionName} already exists for this academic year",
                HttpStatusCode.Conflict
            )
        }

        val classId = classRepository.create(request)
        return getClassById(classId)
    }

    suspend fun getClassById(id: String): ClassDto {
        validateUUID(id, "Class ID")
        return classRepository.findById(id)
            ?: throw ApiException("Class not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllClasses(): List<ClassDto> {
        return classRepository.findAll()
    }

    suspend fun updateClass(id: String, request: UpdateClassRequest): ClassDto {
        validateUUID(id, "Class ID")
        validateClassRequest(request.className, request.sectionName, request.academicYearId)

        // Check if academic year exists
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate class in the same academic year (excluding current class)
        val isDuplicate = classRepository.checkDuplicateClass(
            request.className,
            request.sectionName,
            request.academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "Class ${request.className} Section ${request.sectionName} already exists for this academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = classRepository.update(id, request)
        if (!updated) {
            throw ApiException("Class not found", HttpStatusCode.NotFound)
        }

        return getClassById(id)
    }

    suspend fun deleteClass(id: String) {
        validateUUID(id, "Class ID")
        val deleted = classRepository.delete(id)
        if (!deleted) {
            throw ApiException("Class not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getClassesByAcademicYear(academicYearId: String): List<ClassDto> {
        validateUUID(academicYearId, "Academic Year ID")

        // Check if academic year exists
        academicYearService.getAcademicYearById(academicYearId)

        return classRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getClassesByNameAndSection(className: String, sectionName: String): List<ClassDto> {
        when {
            className.isBlank() -> throw ApiException("Class name cannot be empty", HttpStatusCode.BadRequest)
            sectionName.isBlank() -> throw ApiException("Section name cannot be empty", HttpStatusCode.BadRequest)
        }

        return classRepository.findByClassNameAndSection(className, sectionName)
    }

    private fun validateClassRequest(className: String, sectionName: String, academicYearId: String) {
        when {
            className.isBlank() -> throw ApiException("Class name cannot be empty", HttpStatusCode.BadRequest)
            className.length > 50 -> throw ApiException("Class name is too long (max 50 characters)", HttpStatusCode.BadRequest)
            sectionName.isBlank() -> throw ApiException("Section name cannot be empty", HttpStatusCode.BadRequest)
            sectionName.length > 50 -> throw ApiException("Section name is too long (max 50 characters)", HttpStatusCode.BadRequest)
        }

        validateUUID(academicYearId, "Academic Year ID")
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("$fieldName must be a valid UUID", HttpStatusCode.BadRequest)
        }
    }
}