package com.example.services

import com.example.database.tables.ResultStatus
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.ExamRepository
import io.ktor.http.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class ExamService(
    private val examRepository: ExamRepository,
    private val classService: ClassService,
    private val subjectService: SubjectService,
    private val academicYearService: AcademicYearService
) {

    suspend fun createExam(request: CreateExamRequest): ExamDto {
        println("Create in Service Called")
        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        val requestWithAcademicYear = request.copy(academicYearId = academicYearId)
        validateExamRequest(requestWithAcademicYear)

        // Validate that referenced entities exist
        classService.getClassById(requestWithAcademicYear.classId)
        subjectService.getSubjectById(requestWithAcademicYear.subjectId)
        academicYearService.getAcademicYearById(academicYearId)

        // Check for duplicate
        val isDuplicate = examRepository.checkDuplicate(
            requestWithAcademicYear.name,
            requestWithAcademicYear.classId,
            requestWithAcademicYear.subjectId,
            academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "An exam with this name already exists for this class and subject in the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val examId = examRepository.create(requestWithAcademicYear)
        return getExamById(examId)
    }

    suspend fun bulkCreateExams(request: BulkCreateExamRequest): List<ExamDto> {
        validateUUID(request.classId, "Class ID")

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(academicYearId, "Academic Year ID")

        if (request.exams.isEmpty()) {
            throw ApiException("Exams list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(academicYearId)

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
                academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(CreateExamRequest(
                    name = exam.name,
                    classId = request.classId,
                    subjectId = exam.subjectId,
                    academicYearId = academicYearId,
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

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        val requestWithAcademicYear = request.copy(academicYearId = academicYearId)
        validateExamRequest(requestWithAcademicYear)

        // Validate that referenced entities exist
        classService.getClassById(requestWithAcademicYear.classId)
        subjectService.getSubjectById(requestWithAcademicYear.subjectId)
        academicYearService.getAcademicYearById(academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = examRepository.checkDuplicate(
            requestWithAcademicYear.name,
            requestWithAcademicYear.classId,
            requestWithAcademicYear.subjectId,
            academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "An exam with this name already exists for this class and subject in the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = examRepository.update(id, requestWithAcademicYear)
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

    suspend fun getExamsByClass(classId: String, academicYearId: String? = null): List<ExamDto> {
        validateUUID(classId, "Class ID")
        classService.getClassById(classId)

        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        return examRepository.findByClassAndAcademicYear(classId, finalAcademicYearId)
    }

    suspend fun getExamsBySubject(subjectId: String, academicYearId: String? = null): List<ExamDto> {
        validateUUID(subjectId, "Subject ID")
        subjectService.getSubjectById(subjectId)

        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        return examRepository.findBySubjectAndAcademicYear(subjectId, finalAcademicYearId)
    }

    suspend fun getExamsByClassAndSubject(
        classId: String,
        subjectId: String,
        academicYearId: String? = null
    ): List<ExamDto> {
        validateUUID(classId, "Class ID")
        validateUUID(subjectId, "Subject ID")

        classService.getClassById(classId)
        subjectService.getSubjectById(subjectId)

        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        return examRepository.findByClassAndSubjectAndAcademicYear(
            classId,
            subjectId,
            finalAcademicYearId
        )
    }

    suspend fun getExamsByAcademicYear(academicYearId: String): List<ExamDto> {
        validateUUID(academicYearId, "Academic Year ID")
        academicYearService.getAcademicYearById(academicYearId)
        return examRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getExamsByActiveAcademicYear(): List<ExamDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return examRepository.findByAcademicYear(activeAcademicYear.id)
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

        val finalAcademicYearId = if (academicYearId != null) {
            validateUUID(academicYearId, "Academic Year ID")
            academicYearService.getAcademicYearById(academicYearId)
            academicYearId
        } else {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        return examRepository.findByDateRange(startDate, endDate, finalAcademicYearId)
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
        validateUUID(request.academicYearId!!, "Academic Year ID") // Now guaranteed to be non-null
        validateExamName(request.name)
        validateMaxMarks(request.maxMarks)
        validateDate(request.date)
    }

    private fun validateExamRequest(request: UpdateExamRequest) {
        validateUUID(request.classId, "Class ID")
        validateUUID(request.subjectId, "Subject ID")
        validateUUID(request.academicYearId!!, "Academic Year ID") // Now guaranteed to be non-null
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

    suspend fun getExamsByNameGrouped(examName: String? = null, academicYearId: String? = null): List<ExamByNameDto> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        academicYearService.getAcademicYearById(finalAcademicYearId)

        // Validate exam name if provided
        if (!examName.isNullOrBlank()) {
            if (examName.length > 100) {
                throw ApiException("Exam name cannot exceed 100 characters", HttpStatusCode.BadRequest)
            }
            if (examName.isBlank()) {
                throw ApiException("Exam name cannot be blank", HttpStatusCode.BadRequest)
            }
        }

        return examRepository.getExamsByNameGrouped(examName, finalAcademicYearId)
    }

    suspend fun getExamsName(examName: String? = null, academicYearId: String? = null): List<String> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        academicYearService.getAcademicYearById(finalAcademicYearId)

        // Validate exam name if provided
        if (!examName.isNullOrBlank()) {
            if (examName.length > 100) {
                throw ApiException("Exam name cannot exceed 100 characters", HttpStatusCode.BadRequest)
            }
            if (examName.isBlank()) {
                throw ApiException("Exam name cannot be blank", HttpStatusCode.BadRequest)
            }
        }

        return examRepository.getExamsName(finalAcademicYearId)
    }

    suspend fun getExamsClassesName(examName: String, academicYearId: String? = null): List<ClassByExamNameDto> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        academicYearService.getAcademicYearById(finalAcademicYearId)

        // Validate exam name if provided
        if (examName.isNotBlank()) {
            if (examName.length > 100) {
                throw ApiException("Exam name cannot exceed 100 characters", HttpStatusCode.BadRequest)
            }
            if (examName.isBlank()) {
                throw ApiException("Exam name cannot be blank", HttpStatusCode.BadRequest)
            }
        }

        return examRepository.getExamsClassesName(examName, finalAcademicYearId)
    }

    suspend fun getExamsByClassesAndExamsName(classId:String, examName: String, academicYearId: String? = null): List<ExamDto> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        academicYearService.getAcademicYearById(finalAcademicYearId)

        // Validate exam name if provided
        if (examName.isNotBlank()) {
            if (examName.length > 100) {
                throw ApiException("Exam name cannot exceed 100 characters", HttpStatusCode.BadRequest)
            }
            if (examName.isBlank()) {
                throw ApiException("Exam name cannot be blank", HttpStatusCode.BadRequest)
            }
        }

        return examRepository.getExamsByClassesAndExamsName(classId, examName, finalAcademicYearId)
    }

    suspend fun publishResults(examId: String) {
        val exam = getExamById(examId)

        if (exam.resultStatus != ResultStatus.READY.name) {
            throw ApiException(
                "Results are not ready to publish",
                HttpStatusCode.BadRequest
            )
        }

        examRepository.publishResults(
            examId,
            LocalDateTime.now()
        )
    }


    suspend fun markResultsReady(examId: String) {
        val exam = getExamById(examId)

        if (exam.resultStatus == ResultStatus.PUBLISHED.name) {
            throw ApiException(
                "Results already published",
                HttpStatusCode.BadRequest
            )
        }

        examRepository.updateResultStatus(
            examId,
            ResultStatus.READY
        )
    }


}