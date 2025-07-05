package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.ExamResultRepository
import io.ktor.http.*

class ExamResultService(
    private val examResultRepository: ExamResultRepository,
    private val examService: ExamService,
    private val userService: UserService
) {

    suspend fun createExamResult(request: CreateExamResultRequest): ExamResultDto {
        validateExamResultRequest(request.examId, request.studentId, request.marksObtained)

        // Validate that referenced entities exist
        val exam = examService.getExamById(request.examId)
        val student = userService.getUserById(request.studentId)

        // Validate student is actually a student
        if (student.role != "STUDENT") {
            throw ApiException("User must be a student", HttpStatusCode.BadRequest)
        }

        // Validate marks are within valid range
        if (request.marksObtained < 0 || request.marksObtained > exam.maxMarks!!) {
            throw ApiException(
                "Marks obtained must be between 0 and ${exam.maxMarks}",
                HttpStatusCode.BadRequest
            )
        }

        // Check for duplicate
        val isDuplicate = examResultRepository.checkDuplicate(request.examId, request.studentId)
        if (isDuplicate) {
            throw ApiException(
                "Result already exists for this exam and student combination",
                HttpStatusCode.Conflict
            )
        }

        val examResultId = examResultRepository.create(request)
        return getExamResultById(examResultId)
    }

    suspend fun bulkCreateExamResults(request: BulkCreateExamResultRequest): List<ExamResultDto> {
        validateUUID(request.examId, "Exam ID")

        if (request.results.isEmpty()) {
            throw ApiException("Results list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that exam exists
        val exam = examService.getExamById(request.examId)

        val createRequests = mutableListOf<CreateExamResultRequest>()
        val conflictMessages = mutableListOf<String>()

        for (studentResult in request.results) {
            validateUUID(studentResult.studentId, "Student ID")

            // Validate marks
            if (studentResult.marksObtained < 0 || studentResult.marksObtained > exam.maxMarks!!) {
                conflictMessages.add("Student ${studentResult.studentId}: Marks must be between 0 and ${exam.maxMarks}")
                continue
            }

            val student = try {
                userService.getUserById(studentResult.studentId)
            } catch (e: ApiException) {
                conflictMessages.add("Student ${studentResult.studentId}: Student not found")
                continue
            }

            // Validate student role
            if (student.role != "STUDENT") {
                conflictMessages.add("${student.firstName}: User must be a student")
                continue
            }

            /*// Check for duplicate
            val isDuplicate = examResultRepository.checkDuplicate(request.examId, studentResult.studentId)
            if (isDuplicate) {
                conflictMessages.add("${student.firstName}: Result already exists")
                continue
            }*/

            createRequests.add(CreateExamResultRequest(
                examId = request.examId,
                studentId = studentResult.studentId,
                marksObtained = studentResult.marksObtained,
                grade = studentResult.grade
            ))
        }

        if (createRequests.isEmpty()) {
            val message = if (conflictMessages.isNotEmpty()) {
                "Cannot create results due to conflicts:\n${conflictMessages.joinToString("\n")}"
            } else {
                "All exam results already exist for the specified students"
            }
            throw ApiException(message, HttpStatusCode.Conflict)
        }

        val examResultIds = examResultRepository.bulkCreate(createRequests)
        return examResultIds.map { getExamResultById(it) }
    }

    suspend fun getExamResultById(id: String): ExamResultDto {
        validateUUID(id, "Exam Result ID")
        return examResultRepository.findById(id)
            ?: throw ApiException("Exam result not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllExamResults(): List<ExamResultDto> {
        return examResultRepository.findAll()
    }

    suspend fun updateExamResult(id: String, request: UpdateExamResultRequest): ExamResultDto {
        validateUUID(id, "Exam Result ID")
        validateExamResultRequest(request.examId, request.studentId, request.marksObtained)

        // Validate that referenced entities exist
        val exam = examService.getExamById(request.examId)
        val student = userService.getUserById(request.studentId)

        // Validate student is actually a student
        if (student.role != "STUDENT") {
            throw ApiException("User must be a student", HttpStatusCode.BadRequest)
        }

        // Validate marks are within valid range
        if (request.marksObtained < 0 || request.marksObtained > exam.maxMarks!!) {
            throw ApiException(
                "Marks obtained must be between 0 and ${exam.maxMarks}",
                HttpStatusCode.BadRequest
            )
        }

        // Check for duplicate (excluding current record)
        val isDuplicate = examResultRepository.checkDuplicate(
            request.examId, request.studentId, excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "Result already exists for this exam and student combination",
                HttpStatusCode.Conflict
            )
        }

        val updated = examResultRepository.update(id, request)
        if (!updated) {
            throw ApiException("Exam result not found", HttpStatusCode.NotFound)
        }

        return getExamResultById(id)
    }

    suspend fun deleteExamResult(id: String) {
        validateUUID(id, "Exam Result ID")
        val deleted = examResultRepository.delete(id)
        if (!deleted) {
            throw ApiException("Exam result not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getResultsByExam(examId: String): List<ExamResultDto> {
        validateUUID(examId, "Exam ID")
        // Validate exam exists
        examService.getExamById(examId)
        return examResultRepository.findByExamId(examId)
    }

    suspend fun getResultsByStudent(studentId: String): List<ExamResultDto> {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        val student = userService.getUserById(studentId)
        if (student.role != "STUDENT") {
            throw ApiException("User must be a student", HttpStatusCode.BadRequest)
        }
        return examResultRepository.findByStudentId(studentId)
    }

    suspend fun getResultsByClassAndExam(classId: String, examId: String): List<ExamResultDto> {
        validateUUID(classId, "Class ID")
        validateUUID(examId, "Exam ID")

        // Validate entities exist
        examService.getExamById(examId)
        // Note: Add classService.getClassById(classId) if you have a class service

        return examResultRepository.findByClassAndExam(classId, examId)
    }

    suspend fun getExamsWithResults(academicYearId: String): List<ExamWithResultsDto> {
        validateUUID(academicYearId, "Academic Year ID")
        return examResultRepository.getExamsWithResults(academicYearId)
    }

    suspend fun getStudentsWithExamResults(academicYearId: String): List<StudentWithExamResultsDto> {
        validateUUID(academicYearId, "Academic Year ID")
        return examResultRepository.getStudentsWithExamResults(academicYearId)
    }

    suspend fun getClassResultSummary(classId: String, examId: String): ClassResultSummaryDto {
        validateUUID(classId, "Class ID")
        validateUUID(examId, "Exam ID")

        return examResultRepository.getClassResultSummary(classId, examId)
            ?: throw ApiException("No results found for this class and exam combination", HttpStatusCode.NotFound)
    }

    suspend fun removeAllResultsForExam(examId: String): Int {
        validateUUID(examId, "Exam ID")
        // Validate exam exists
        examService.getExamById(examId)
        return examResultRepository.deleteByExamId(examId)
    }

    suspend fun removeAllResultsForStudent(studentId: String): Int {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        val student = userService.getUserById(studentId)
        if (student.role != "STUDENT") {
            throw ApiException("User must be a student", HttpStatusCode.BadRequest)
        }
        return examResultRepository.deleteByStudentId(studentId)
    }

    suspend fun getStudentReportByExamName(examName: String): List<StudentExamReportDto> {
        if (examName.isBlank()) {
            throw ApiException("Exam name cannot be empty", HttpStatusCode.BadRequest)
        }
        return examResultRepository.getStudentReportByExamName(examName)
    }

    suspend fun getStudentReportByExamNameAndClass(examName: String, classId: String): ExamReportResponseDto {
        if (examName.isBlank()) {
            throw ApiException("Exam name cannot be empty", HttpStatusCode.BadRequest)
        }
        validateUUID(classId, "Class ID")
        return examResultRepository.getStudentReportByExamNameAndClass(examName, classId)
    }

    private fun validateExamResultRequest(examId: String, studentId: String, marksObtained: Int) {
        validateUUID(examId, "Exam ID")
        validateUUID(studentId, "Student ID")

        if (marksObtained < 0) {
            throw ApiException("Marks obtained cannot be negative", HttpStatusCode.BadRequest)
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