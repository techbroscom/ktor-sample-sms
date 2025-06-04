package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.ExamScheduleRepository
import io.ktor.http.*
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class ExamScheduleService(
    private val examScheduleRepository: ExamScheduleRepository,
    private val examService: ExamService,
    private val classService: ClassService
) {

    suspend fun createExamSchedule(request: CreateExamScheduleRequest): ExamScheduleDto {
        validateExamScheduleRequest(request.examId, request.classId, request.startTime, request.endTime)

        // Validate that referenced entities exist
        examService.getExamById(request.examId)
        classService.getClassById(request.classId)

        val startTime = parseDateTime(request.startTime, "Start time")
        val endTime = parseDateTime(request.endTime, "End time")

        // Validate time range
        if (startTime >= endTime) {
            throw ApiException("Start time must be before end time", HttpStatusCode.BadRequest)
        }

        // Check for duplicate
        val isDuplicate = examScheduleRepository.checkDuplicate(request.examId, request.classId)
        if (isDuplicate) {
            throw ApiException(
                "Schedule already exists for this exam and class combination",
                HttpStatusCode.Conflict
            )
        }

        // Check for time conflicts
        val hasTimeConflict = examScheduleRepository.checkTimeConflict(
            request.classId, startTime, endTime
        )
        if (hasTimeConflict) {
            throw ApiException(
                "Time conflict: Another exam is scheduled for this class during the specified time",
                HttpStatusCode.Conflict
            )
        }

        val examScheduleId = examScheduleRepository.create(request)
        return getExamScheduleById(examScheduleId)
    }

    suspend fun bulkCreateExamSchedules(request: BulkCreateExamScheduleRequest): List<ExamScheduleDto> {
        validateUUID(request.examId, "Exam ID")
        validateDateTime(request.startTime, "Start time")
        validateDateTime(request.endTime, "End time")

        if (request.classIds.isEmpty()) {
            throw ApiException("Class IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        val startTime = parseDateTime(request.startTime, "Start time")
        val endTime = parseDateTime(request.endTime, "End time")

        // Validate time range
        if (startTime >= endTime) {
            throw ApiException("Start time must be before end time", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        examService.getExamById(request.examId)

        val createRequests = mutableListOf<CreateExamScheduleRequest>()
        val conflictMessages = mutableListOf<String>()

        for (classId in request.classIds) {
            validateUUID(classId, "Class ID")
            val classDto = classService.getClassById(classId)

            // Check for duplicate
            val isDuplicate = examScheduleRepository.checkDuplicate(request.examId, classId)
            if (isDuplicate) {
                conflictMessages.add("${classDto.className} ${classDto.sectionName}: Schedule already exists")
                continue
            }

            // Check for time conflicts
            val hasTimeConflict = examScheduleRepository.checkTimeConflict(classId, startTime, endTime)
            if (hasTimeConflict) {
                conflictMessages.add("${classDto.className} ${classDto.sectionName}: Time conflict with existing exam")
                continue
            }

            createRequests.add(CreateExamScheduleRequest(
                examId = request.examId,
                classId = classId,
                startTime = request.startTime,
                endTime = request.endTime
            ))
        }

        if (createRequests.isEmpty()) {
            val message = if (conflictMessages.isNotEmpty()) {
                "Cannot create schedules due to conflicts:\n${conflictMessages.joinToString("\n")}"
            } else {
                "All exam schedules already exist for the specified classes"
            }
            throw ApiException(message, HttpStatusCode.Conflict)
        }

        val examScheduleIds = examScheduleRepository.bulkCreate(createRequests)
        val result = examScheduleIds.map { getExamScheduleById(it) }

        // Include warning about conflicts if any
        if (conflictMessages.isNotEmpty()) {
            // You might want to log these conflicts or include them in the response
            // For now, we'll just return the successfully created schedules
        }

        return result
    }

    suspend fun getExamScheduleById(id: String): ExamScheduleDto {
        validateUUID(id, "Exam Schedule ID")
        return examScheduleRepository.findById(id)
            ?: throw ApiException("Exam schedule not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllExamSchedules(): List<ExamScheduleDto> {
        return examScheduleRepository.findAll()
    }

    suspend fun updateExamSchedule(id: String, request: UpdateExamScheduleRequest): ExamScheduleDto {
        validateUUID(id, "Exam Schedule ID")
        validateExamScheduleRequest(request.examId, request.classId, request.startTime, request.endTime)

        // Validate that referenced entities exist
        examService.getExamById(request.examId)
        classService.getClassById(request.classId)

        val startTime = parseDateTime(request.startTime, "Start time")
        val endTime = parseDateTime(request.endTime, "End time")

        // Validate time range
        if (startTime >= endTime) {
            throw ApiException("Start time must be before end time", HttpStatusCode.BadRequest)
        }

        // Check for duplicate (excluding current record)
        val isDuplicate = examScheduleRepository.checkDuplicate(
            request.examId, request.classId, excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "Schedule already exists for this exam and class combination",
                HttpStatusCode.Conflict
            )
        }

        // Check for time conflicts (excluding current record)
        val hasTimeConflict = examScheduleRepository.checkTimeConflict(
            request.classId, startTime, endTime, excludeId = id
        )
        if (hasTimeConflict) {
            throw ApiException(
                "Time conflict: Another exam is scheduled for this class during the specified time",
                HttpStatusCode.Conflict
            )
        }

        val updated = examScheduleRepository.update(id, request)
        if (!updated) {
            throw ApiException("Exam schedule not found", HttpStatusCode.NotFound)
        }

        return getExamScheduleById(id)
    }

    suspend fun deleteExamSchedule(id: String) {
        validateUUID(id, "Exam Schedule ID")
        val deleted = examScheduleRepository.delete(id)
        if (!deleted) {
            throw ApiException("Exam schedule not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getSchedulesByExam(examId: String): List<ExamScheduleDto> {
        validateUUID(examId, "Exam ID")
        // Validate exam exists
        examService.getExamById(examId)
        return examScheduleRepository.findByExamId(examId)
    }

    suspend fun getSchedulesByClass(classId: String): List<ExamScheduleDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return examScheduleRepository.findByClassId(classId)
    }

    suspend fun getSchedulesByDateRange(startDate: String, endDate: String): List<ExamScheduleDto> {
        val startDateTime = parseDateTime(startDate, "Start date")
        val endDateTime = parseDateTime(endDate, "End date")

        if (startDateTime >= endDateTime) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        return examScheduleRepository.findByDateRange(startDateTime, endDateTime)
    }

    suspend fun getExamsWithSchedules(academicYearId: String): List<ExamWithSchedulesDto> {
        validateUUID(academicYearId, "Academic Year ID")
        return examScheduleRepository.getExamsWithSchedules(academicYearId)
    }

    suspend fun getClassesWithExamSchedules(academicYearId: String): List<ClassWithExamSchedulesDto> {
        validateUUID(academicYearId, "Academic Year ID")
        return examScheduleRepository.getClassesWithExamSchedules(academicYearId)
    }

    suspend fun removeAllSchedulesForExam(examId: String): Int {
        validateUUID(examId, "Exam ID")
        // Validate exam exists
        examService.getExamById(examId)
        return examScheduleRepository.deleteByExamId(examId)
    }

    suspend fun removeAllSchedulesForClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return examScheduleRepository.deleteByClassId(classId)
    }

    private fun validateExamScheduleRequest(examId: String, classId: String, startTime: String, endTime: String) {
        validateUUID(examId, "Exam ID")
        validateUUID(classId, "Class ID")
        validateDateTime(startTime, "Start time")
        validateDateTime(endTime, "End time")
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("$fieldName must be a valid UUID", HttpStatusCode.BadRequest)
        }
    }

    private fun validateDateTime(dateTime: String, fieldName: String) {
        try {
            LocalDateTime.parse(dateTime)
        } catch (e: DateTimeParseException) {
            throw ApiException("$fieldName must be a valid ISO datetime format", HttpStatusCode.BadRequest)
        }
    }

    private fun parseDateTime(dateTime: String, fieldName: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTime)
        } catch (e: DateTimeParseException) {
            throw ApiException("$fieldName must be a valid ISO datetime format", HttpStatusCode.BadRequest)
        }
    }
}
