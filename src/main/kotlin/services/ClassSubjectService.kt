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
        validateClassSubjectRequest(request.classId, request.subjectId)

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        subjectService.getSubjectById(request.subjectId)
        academicYearService.getAcademicYearById(academicYearId)

        // Check for duplicate
        val isDuplicate = classSubjectRepository.checkDuplicate(
            request.classId,
            request.subjectId,
            academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "This subject is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val classSubjectId = classSubjectRepository.create(request.copy(academicYearId = academicYearId))
        return getClassSubjectById(classSubjectId)
    }

    suspend fun bulkCreateClassSubjects(request: BulkCreateClassSubjectRequest): List<ClassSubjectDto> {
        validateUUID(request.classId, "Class ID")

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        if (request.subjectIds.isEmpty()) {
            throw ApiException("Subject IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(academicYearId)

        val createRequests = mutableListOf<CreateClassSubjectRequest>()

        for (subjectId in request.subjectIds) {
            validateUUID(subjectId, "Subject ID")
            subjectService.getSubjectById(subjectId)

            // Check for duplicate
            val isDuplicate = classSubjectRepository.checkDuplicate(
                request.classId,
                subjectId,
                academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(CreateClassSubjectRequest(
                    classId = request.classId,
                    subjectId = subjectId,
                    academicYearId = academicYearId
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

    suspend fun getClassSubjectsForActiveYear(): List<ClassSubjectDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return classSubjectRepository.findByAcademicYear(activeAcademicYear.id)
    }

    suspend fun updateClassSubject(id: String, request: UpdateClassSubjectRequest): ClassSubjectDto {
        validateUUID(id, "Class Subject ID")
        validateClassSubjectRequest(request.classId, request.subjectId)

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        subjectService.getSubjectById(request.subjectId)
        academicYearService.getAcademicYearById(academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = classSubjectRepository.checkDuplicate(
            request.classId,
            request.subjectId,
            academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "This subject is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = classSubjectRepository.update(id, request.copy(academicYearId = academicYearId))
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

    suspend fun getSubjectsByClassForActiveYear(classId: String): List<ClassSubjectDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return classSubjectRepository.findByClassAndAcademicYear(classId, activeAcademicYear.id)
    }

    suspend fun getClassesBySubject(subjectId: String): List<ClassSubjectDto> {
        validateUUID(subjectId, "Subject ID")
        // Validate subject exists
        subjectService.getSubjectById(subjectId)
        return classSubjectRepository.findBySubjectId(subjectId)
    }

    suspend fun getClassesBySubjectForActiveYear(subjectId: String): List<ClassSubjectDto> {
        validateUUID(subjectId, "Subject ID")
        // Validate subject exists
        subjectService.getSubjectById(subjectId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return classSubjectRepository.findBySubjectAndAcademicYear(subjectId, activeAcademicYear.id)
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

    suspend fun getClassesWithSubjectsForActiveYear(): List<ClassWithSubjectsDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return classSubjectRepository.getClassesWithSubjects(activeAcademicYear.id)
    }

    suspend fun getSubjectsWithClasses(academicYearId: String): List<SubjectWithClassesDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return classSubjectRepository.getSubjectsWithClasses(academicYearId)
    }

    suspend fun getSubjectsWithClassesForActiveYear(): List<SubjectWithClassesDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return classSubjectRepository.getSubjectsWithClasses(activeAcademicYear.id)
    }

    suspend fun removeAllSubjectsFromClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return classSubjectRepository.deleteByClassId(classId)
    }

    suspend fun removeAllSubjectsFromClassForActiveYear(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return classSubjectRepository.deleteByClassAndAcademicYear(classId, activeAcademicYear.id)
    }

    suspend fun removeClassFromAllSubjects(subjectId: String): Int {
        validateUUID(subjectId, "Subject ID")
        // Validate subject exists
        subjectService.getSubjectById(subjectId)
        return classSubjectRepository.deleteBySubjectId(subjectId)
    }

    suspend fun removeClassFromAllSubjectsForActiveYear(subjectId: String): Int {
        validateUUID(subjectId, "Subject ID")
        // Validate subject exists
        subjectService.getSubjectById(subjectId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return classSubjectRepository.deleteBySubjectAndAcademicYear(subjectId, activeAcademicYear.id)
    }

    private fun validateClassSubjectRequest(classId: String, subjectId: String) {
        validateUUID(classId, "Class ID")
        validateUUID(subjectId, "Subject ID")
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("$fieldName must be a valid UUID", HttpStatusCode.BadRequest)
        }
    }
}