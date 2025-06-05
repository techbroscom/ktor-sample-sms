package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreateSubjectRequest
import com.example.models.dto.SubjectDto
import com.example.models.dto.UpdateSubjectRequest
import com.example.repositories.SubjectRepository
import io.ktor.http.*

class SubjectService(private val subjectRepository: SubjectRepository) {

    suspend fun createSubject(request: CreateSubjectRequest): SubjectDto {
        validateSubjectRequest(request.name, request.code)

        /*// Check for duplicate code
        val codeExists = subjectRepository.checkDuplicateCode(request.code)
        if (codeExists) {
            throw ApiException("Subject code '${request.code}' already exists", HttpStatusCode.Conflict)
        }*/

        // Check for duplicate name (optional - you can remove this if names can be duplicated)
        val nameExists = subjectRepository.checkDuplicateName(request.name)
        if (nameExists) {
            throw ApiException("Subject name '${request.name}' already exists", HttpStatusCode.Conflict)
        }

        val subjectId = subjectRepository.create(request)
        return getSubjectById(subjectId)
    }

    suspend fun getSubjectById(id: String): SubjectDto {
        validateUUID(id, "Subject ID")
        return subjectRepository.findById(id)
            ?: throw ApiException("Subject not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllSubjects(): List<SubjectDto> {
        return subjectRepository.findAll()
    }

    suspend fun updateSubject(id: String, request: UpdateSubjectRequest): SubjectDto {
        validateUUID(id, "Subject ID")
        validateSubjectRequest(request.name, request.code)

        // Check for duplicate code (excluding current subject)
        val codeExists = subjectRepository.checkDuplicateCode(request.code, excludeId = id)
        if (codeExists) {
            throw ApiException("Subject code '${request.code}' already exists", HttpStatusCode.Conflict)
        }

        // Check for duplicate name (excluding current subject)
        val nameExists = subjectRepository.checkDuplicateName(request.name, excludeId = id)
        if (nameExists) {
            throw ApiException("Subject name '${request.name}' already exists", HttpStatusCode.Conflict)
        }

        val updated = subjectRepository.update(id, request)
        if (!updated) {
            throw ApiException("Subject not found", HttpStatusCode.NotFound)
        }

        return getSubjectById(id)
    }

    suspend fun deleteSubject(id: String) {
        validateUUID(id, "Subject ID")
        val deleted = subjectRepository.delete(id)
        if (!deleted) {
            throw ApiException("Subject not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getSubjectByCode(code: String): SubjectDto {
        if (code.isBlank()) {
            throw ApiException("Subject code cannot be empty", HttpStatusCode.BadRequest)
        }

        return subjectRepository.findByCode(code)
            ?: throw ApiException("Subject with code '$code' not found", HttpStatusCode.NotFound)
    }

    suspend fun searchSubjectsByName(name: String): List<SubjectDto> {
        if (name.isBlank()) {
            throw ApiException("Search name cannot be empty", HttpStatusCode.BadRequest)
        }

        return subjectRepository.findByName(name)
    }

    suspend fun getSubjectsPaginated(page: Int, pageSize: Int): Pair<List<SubjectDto>, Long> {
        if (page < 1) {
            throw ApiException("Page number must be greater than 0", HttpStatusCode.BadRequest)
        }

        if (pageSize < 1 || pageSize > 100) {
            throw ApiException("Page size must be between 1 and 100", HttpStatusCode.BadRequest)
        }

        val offset = (page - 1) * pageSize.toLong()
        val subjects = subjectRepository.findPaginated(offset, pageSize)
        val totalCount = subjectRepository.getTotalCount()

        return Pair(subjects, totalCount)
    }

    private fun validateSubjectRequest(name: String, code: String?) {
        when {
            name.isBlank() -> throw ApiException("Subject name cannot be empty", HttpStatusCode.BadRequest)
            name.length > 100 -> throw ApiException("Subject name is too long (max 100 characters)", HttpStatusCode.BadRequest)
            /*code.isBlank() -> throw ApiException("Subject code cannot be empty", HttpStatusCode.BadRequest)
            code.length > 20 -> throw ApiException("Subject code is too long (max 20 characters)", HttpStatusCode.BadRequest)
            !code.matches(Regex("^[A-Z0-9_-]+$")) -> throw ApiException("Subject code must contain only uppercase letters, numbers, hyphens and underscores", HttpStatusCode.BadRequest)*/
        }
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("$fieldName must be a valid UUID", HttpStatusCode.BadRequest)
        }
    }
}