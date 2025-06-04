package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.ClassSubjectRepository
import io.ktor.http.*

class ClassSubjectService(
    private val classSubjectRepository: ClassSubjectRepository,
    private val classService: ClassService,
    private val subjectService: SubjectService,
    private val academicYearService: AcademicYearService
) {

    suspend fun createClassSubject(request: CreateClassSubjectRequest): ClassSubjectDto {
        validateClassSubjectRequest(request.classId, request.subjectId, request.academicYearId)

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        subjectService.getSubjectById(request.subjectId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate
        val isDuplicate = classSubjectRepository.checkDuplicate(
            request.classId,
            request.subjectId,
            request.academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "This subject is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val classSubjectId = classSubjectRepository.create(request)
        return getClassSubjectById(classSubjectId)
    }

    suspend fun bulkCreateClassSubjects(request: BulkCreateClassSubjectRequest): List<ClassSubjectDto> {
        validateUUID(request.classId, "Class ID")
        validateUUID(request.academicYearId, "Academic Year ID")

        if (request.subjectIds.isEmpty()) {
            throw ApiException("Subject IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        val createRequests = mutableListOf<CreateClassSubjectRequest>()

        for (subjectId in request.subjectIds) {
            validateUUID(subjectId, "Subject ID")
            subjectService.getSubjectById(subjectId)

            // Check for duplicate
            val isDuplicate = classSubjectRepository.checkDuplicate(
                request.classId,
                subjectId,
                request.academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(CreateClassSubjectRequest(
                    classId = request.classId,
                    subjectId = subjectId,
                    academicYearId = request.academicYearId
                ))
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException("All subjects are already assigned to this class", HttpStatusCode.Conflict)
        }

        val classSubjectIds = classSubjectRepository.bulkCreate(createRequests)
        return classSubjectIds.map { getClassSubjectById(it) }
    }

    suspend fun getClassSubjectById(id: String): ClassSubjectDto {
        validateUUID(id, "Class Subject ID")
        return classSubjectRepository.findById(id)
            ?: throw ApiException("Class Subject assignment not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllClassSubjects(): List<ClassSubjectDto> {
        return classSubjectRepository.findAll()
    }

    suspend fun updateClassSubject(id: String, request: UpdateClassSubjectRequest): ClassSubjectDto {
        validateUUID(id, "Class Subject ID")
        validateClassSubjectRequest(request.classId, request.subjectId, request.academicYearId)

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        subjectService.getSubjectById(request.subjectId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = classSubjectRepository.checkDuplicate(
            request.classId,
            request.subjectId,
            request.academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "This subject is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = classSubjectRepository.update(id, request)
        if (!updated) {
            throw ApiException("Class Subject assignment not found", HttpStatusCode.NotFound)
        }

        return getClassSubjectById(id)
    }

    suspend fun deleteClassSubject(id: String) {
        validateUUID(id, "Class Subject ID")
        val deleted = classSubjectRepository.delete(id)
        if (!deleted) {
            throw ApiException("Class Subject assignment not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getSubjectsByClass(classId: String): List<ClassSubjectDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return classSubjectRepository.findByClassId(classId)
    }

    suspend fun getClassesBySubject(subjectId: String): List<ClassSubjectDto> {
        validateUUID(subjectId, "Subject ID")
        // Validate subject exists
        subjectService.getSubjectById(subjectId)
        return classSubjectRepository.findBySubjectId(subjectId)
    }

    suspend fun getClassSubjectsByAcademicYear(academicYearId: String): List<ClassSubjectDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return classSubjectRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getSubjectsByClassAndAcademicYear(classId: String, academicYearId: String): List<ClassSubjectDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return classSubjectRepository.findByClassAndAcademicYear(classId, academicYearId)
    }

    suspend fun getClassesWithSubjects(academicYearId: String): List<ClassWithSubjectsDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return classSubjectRepository.getClassesWithSubjects(academicYearId)
    }

    suspend fun getSubjectsWithClasses(academicYearId: String): List<SubjectWithClassesDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return classSubjectRepository.getSubjectsWithClasses(academicYearId)
    }

    suspend fun removeAllSubjectsFromClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return classSubjectRepository.deleteByClassId(classId)
    }

    suspend fun removeClassFromAllSubjects(subjectId: String): Int {
        validateUUID(subjectId, "Subject ID")
        // Validate subject exists
        subjectService.getSubjectById(subjectId)
        return classSubjectRepository.deleteBySubjectId(subjectId)
    }

    private fun validateClassSubjectRequest(classId: String, subjectId: String, academicYearId: String) {
        validateUUID(classId, "Class ID")
        validateUUID(subjectId, "Subject ID")
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