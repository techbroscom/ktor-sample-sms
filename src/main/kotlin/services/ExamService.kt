package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.ExamRepository
import io.ktor.http.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

class ExamService(
    private val examRepository: ExamRepository,
    private val classService: ClassService,
    private val subjectService: SubjectService,
    private val academicYearService: AcademicYearService
) {

    suspend fun createExam(request: CreateExamRequest): ExamDto {
        validateExamRequest(request)

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        subjectService.getSubjectById(request.subjectId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate
        val isDuplicate = examRepository.checkDuplicate(
            request.name,
            request.classId,
            request.subjectId,
            request.academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "An exam with this name already exists for this class and subject in the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val examId = examRepository.create(request)
        return getExamById(examId)
    }

    suspend fun bulkCreateExams(request: BulkCreateExamRequest): List<ExamDto> {
        validateUUID(request.classId, "Class ID")
        validateUUID(request.academicYearId, "Academic Year ID")

        if (request.exams.isEmpty()) {
            throw ApiException("Exams list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        val createRequests = mutableListOf<CreateExamRequest>()

        for (exam in request.exams) {
            validateUUID(exam.subjectId, "Subject ID")
            validateExamName(exam.name)
            validateMaxMarks(exam.maxMarks)
            validateDate(exam.date)

            subjectService.getSubjectById(exam.subjectId)

            // Check for duplicate
            val isDuplicate = examRepository.checkDuplicate(
                exam.name,
                request.classId,
                exam.subjectId,
                request.academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(CreateExamRequest(
                    name = exam.name,
                    classId = request.classId,
                    subjectId = exam.subjectId,
                    academicYearId = request.academicYearId,
                    maxMarks = exam.maxMarks,
                    date = exam.date
                ))
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException("All exams already exist for this class", HttpStatusCode.Conflict)
        }

        val examIds = examRepository.bulkCreate(createRequests)
        return examIds.map { getExamById(it) }
    }

    suspend fun getExamById(id: String): ExamDto {
        validateUUID(id, "Exam ID")
        return examRepository.findById(id)
            ?: throw ApiException("Exam not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllExams(): List<ExamDto> {
        return examRepository.findAll()
    }

    suspend fun updateExam(id: String, request: UpdateExamRequest): ExamDto {
        validateUUID(id, "Exam ID")
        validateExamRequest(request)

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        subjectService.getSubjectById(request.subjectId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = examRepository.checkDuplicate(
            request.name,
            request.classId,
            request.subjectId,
            request.academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "An exam with this name already exists for this class and subject in the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = examRepository.update(id, request)
        if (!updated) {
            throw ApiException("Exam not found", HttpStatusCode.NotFound)
        }

        return getExamById(id)
    }

    suspend fun deleteExam(id: String) {
        validateUUID(id, "Exam ID")
        val deleted = examRepository.delete(id)
        if (!deleted) {
            throw ApiException("Exam not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getExamsByClass(classId: String): List<ExamDto> {
        validateUUID(classId, "Class ID")
        classService.getClassById(classId)
        return examRepository.findByClassId(classId)
    }

    suspend fun getExamsBySubject(subjectId: String): List<ExamDto> {
        validateUUID(subjectId, "Subject ID")
        subjectService.getSubjectById(subjectId)
        return examRepository.findBySubjectId(subjectId)
    }

    suspend fun getExamsByAcademicYear(academicYearId: String): List<ExamDto> {
        validateUUID(academicYearId, "Academic Year ID")
        academicYearService.getAcademicYearById(academicYearId)
        return examRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getExamsByClassAndAcademicYear(classId: String, academicYearId: String): List<ExamDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return examRepository.findByClassAndAcademicYear(classId, academicYearId)
    }

    suspend fun getExamsBySubjectAndAcademicYear(subjectId: String, academicYearId: String): List<ExamDto> {
        validateUUID(subjectId, "Subject ID")
        validateUUID(academicYearId, "Academic Year ID")

        subjectService.getSubjectById(subjectId)
        academicYearService.getAcademicYearById(academicYearId)

        return examRepository.findBySubjectAndAcademicYear(subjectId, academicYearId)
    }

    suspend fun getExamsByDateRange(startDate: String, endDate: String, academicYearId: String? = null): List<ExamDto> {
        validateDate(startDate)
        validateDate(endDate)

        if (academicYearId != null) {
            validateUUID(academicYearId, "Academic Year ID")
            academicYearService.getAcademicYearById(academicYearId)
        }

        return examRepository.findByDateRange(startDate, endDate, academicYearId)
    }

    suspend fun removeAllExamsFromClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        classService.getClassById(classId)
        return examRepository.deleteByClassId(classId)
    }

    suspend fun removeAllExamsFromSubject(subjectId: String): Int {
        validateUUID(subjectId, "Subject ID")
        subjectService.getSubjectById(subjectId)
        return examRepository.deleteBySubjectId(subjectId)
    }

    private fun validateExamRequest(request: CreateExamRequest) {
        validateUUID(request.classId, "Class ID")
        validateUUID(request.subjectId, "Subject ID")
        validateUUID(request.academicYearId, "Academic Year ID")
        validateExamName(request.name)
        validateMaxMarks(request.maxMarks)
        validateDate(request.date)
    }

    private fun validateExamRequest(request: UpdateExamRequest) {
        validateUUID(request.classId, "Class ID")
        validateUUID(request.subjectId, "Subject ID")
        validateUUID(request.academicYearId, "Academic Year ID")
        validateExamName(request.name)
        validateMaxMarks(request.maxMarks)
        validateDate(request.date)
    }

    private fun validateExamName(name: String) {
        if (name.isBlank()) {
            throw ApiException("Exam name cannot be blank", HttpStatusCode.BadRequest)
        }
        if (name.length > 100) {
            throw ApiException("Exam name cannot exceed 100 characters", HttpStatusCode.BadRequest)
        }
    }

    private fun validateMaxMarks(maxMarks: Int) {
        if (maxMarks <= 0) {
            throw ApiException("Max marks must be greater than 0", HttpStatusCode.BadRequest)
        }
        if (maxMarks > 10000) {
            throw ApiException("Max marks cannot exceed 10000", HttpStatusCode.BadRequest)
        }
    }

    private fun validateDate(dateString: String) {
        try {
            LocalDate.parse(dateString)
        } catch (e: DateTimeParseException) {
            throw ApiException("Date must be in YYYY-MM-DD format", HttpStatusCode.BadRequest)
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