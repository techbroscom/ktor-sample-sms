package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.AttendanceRepository
import io.ktor.http.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

class AttendanceService(
    private val attendanceRepository: AttendanceRepository,
    private val userService: UserService,
    private val classService: ClassService
) {

    suspend fun createAttendance(request: CreateAttendanceRequest): AttendanceDto {
        validateAttendanceRequest(request.studentId, request.classId, request.date)

        // Validate that referenced entities exist
        userService.getUserById(request.studentId)
        classService.getClassById(request.classId)

        val date = parseDate(request.date)

        // Check for duplicate
        val isDuplicate = attendanceRepository.checkDuplicate(
            request.studentId,
            request.classId,
            date
        )
        if (isDuplicate) {
            throw ApiException(
                "Attendance record already exists for this student, class, and date",
                HttpStatusCode.Conflict
            )
        }

        val attendanceId = attendanceRepository.create(request)
        return getAttendanceById(attendanceId)
    }

    suspend fun bulkCreateAttendance(request: BulkCreateAttendanceRequest): List<AttendanceDto> {
        validateUUID(request.classId, "Class ID")
        validateDateString(request.date, "Date")

        if (request.attendanceRecords.isEmpty()) {
            throw ApiException("Attendance records list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that class exists
        classService.getClassById(request.classId)
        val date = parseDate(request.date)

        val createRequests = mutableListOf<CreateAttendanceRequest>()

        for (record in request.attendanceRecords) {
            validateUUID(record.studentId, "Student ID")
            userService.getUserById(record.studentId)

            // Check for duplicate
            val isDuplicate = attendanceRepository.checkDuplicate(
                record.studentId,
                request.classId,
                date
            )
            if (!isDuplicate) {
                createRequests.add(CreateAttendanceRequest(
                    studentId = record.studentId,
                    classId = request.classId,
                    date = request.date,
                    status = record.status
                ))
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException("All attendance records already exist for this date", HttpStatusCode.Conflict)
        }

        val attendanceIds = attendanceRepository.bulkCreate(createRequests)
        return attendanceIds.map { getAttendanceById(it) }
    }

    suspend fun getAttendanceById(id: String): AttendanceDto {
        validateUUID(id, "Attendance ID")
        return attendanceRepository.findById(id)
            ?: throw ApiException("Attendance record not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllAttendance(): List<AttendanceDto> {
        return attendanceRepository.findAll()
    }

    suspend fun updateAttendance(id: String, request: UpdateAttendanceRequest): AttendanceDto {
        validateUUID(id, "Attendance ID")
        validateAttendanceRequest(request.studentId, request.classId, request.date)

        // Validate that referenced entities exist
        userService.getUserById(request.studentId)
        classService.getClassById(request.classId)

        val date = parseDate(request.date)

        // Check for duplicate (excluding current record)
        val isDuplicate = attendanceRepository.checkDuplicate(
            request.studentId,
            request.classId,
            date,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "Attendance record already exists for this student, class, and date",
                HttpStatusCode.Conflict
            )
        }

        val updated = attendanceRepository.update(id, request)
        if (!updated) {
            throw ApiException("Attendance record not found", HttpStatusCode.NotFound)
        }

        return getAttendanceById(id)
    }

    suspend fun deleteAttendance(id: String) {
        validateUUID(id, "Attendance ID")
        val deleted = attendanceRepository.delete(id)
        if (!deleted) {
            throw ApiException("Attendance record not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getAttendanceByStudent(studentId: String): List<AttendanceDto> {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        userService.getUserById(studentId)
        return attendanceRepository.findByStudentId(studentId)
    }

    suspend fun getAttendanceByClass(classId: String): List<AttendanceDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return attendanceRepository.findByClassId(classId)
    }

    suspend fun getAttendanceByDate(date: String): List<AttendanceDto> {
        validateDateString(date, "Date")
        val localDate = parseDate(date)
        return attendanceRepository.findByDate(localDate)
    }

    suspend fun getAttendanceByClassAndDate(classId: String, date: String): List<AttendanceDto> {
        validateUUID(classId, "Class ID")
        validateDateString(date, "Date")

        // Validate class exists
        classService.getClassById(classId)
        val localDate = parseDate(date)

        return attendanceRepository.findByClassAndDate(classId, localDate)
    }

    suspend fun getAttendanceByStudentAndDateRange(studentId: String, startDate: String, endDate: String): List<AttendanceDto> {
        validateUUID(studentId, "Student ID")
        validateDateString(startDate, "Start Date")
        validateDateString(endDate, "End Date")

        // Validate student exists
        userService.getUserById(studentId)

        val start = parseDate(startDate)
        val end = parseDate(endDate)

        if (start.isAfter(end)) {
            throw ApiException("Start date cannot be after end date", HttpStatusCode.BadRequest)
        }

        return attendanceRepository.findByStudentAndDateRange(studentId, start, end)
    }

    suspend fun getAttendanceByClassAndDateRange(classId: String, startDate: String, endDate: String): List<AttendanceDto> {
        validateUUID(classId, "Class ID")
        validateDateString(startDate, "Start Date")
        validateDateString(endDate, "End Date")

        // Validate class exists
        classService.getClassById(classId)

        val start = parseDate(startDate)
        val end = parseDate(endDate)

        if (start.isAfter(end)) {
            throw ApiException("Start date cannot be after end date", HttpStatusCode.BadRequest)
        }

        return attendanceRepository.findByClassAndDateRange(classId, start, end)
    }

    suspend fun getAttendanceStats(studentId: String, startDate: String, endDate: String): StudentAttendanceStatsDto {
        validateUUID(studentId, "Student ID")
        validateDateString(startDate, "Start Date")
        validateDateString(endDate, "End Date")

        // Validate student exists
        val student = userService.getUserById(studentId)

        val start = parseDate(startDate)
        val end = parseDate(endDate)

        if (start.isAfter(end)) {
            throw ApiException("Start date cannot be after end date", HttpStatusCode.BadRequest)
        }

        val stats = attendanceRepository.getAttendanceStats(studentId, start, end)

        return StudentAttendanceStatsDto(
            studentId = studentId,
            studentName = "${student.firstName} ${student.lastName}".trim(),
            studentEmail = student.email,
            stats = stats
        )
    }

    suspend fun getClassAttendanceForDate(classId: String, date: String): ClassAttendanceDto {
        validateUUID(classId, "Class ID")
        validateDateString(date, "Date")

        // Validate class exists
        classService.getClassById(classId)
        val localDate = parseDate(date)

        return attendanceRepository.getClassAttendanceForDate(classId, localDate)
    }

    suspend fun removeAllAttendanceForStudent(studentId: String): Int {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        userService.getUserById(studentId)
        return attendanceRepository.deleteByStudentId(studentId)
    }

    suspend fun removeAllAttendanceForClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return attendanceRepository.deleteByClassId(classId)
    }

    suspend fun removeAllAttendanceForDate(date: String): Int {
        validateDateString(date, "Date")
        val localDate = parseDate(date)
        return attendanceRepository.deleteByDate(localDate)
    }

    private fun validateAttendanceRequest(studentId: String, classId: String, date: String) {
        validateUUID(studentId, "Student ID")
        validateUUID(classId, "Class ID")
        validateDateString(date, "Date")
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("$fieldName must be a valid UUID", HttpStatusCode.BadRequest)
        }
    }

    private fun validateDateString(dateString: String, fieldName: String) {
        try {
            LocalDate.parse(dateString)
        } catch (e: DateTimeParseException) {
            throw ApiException("$fieldName must be in YYYY-MM-DD format", HttpStatusCode.BadRequest)
        }
    }

    private fun parseDate(dateString: String): LocalDate {
        return try {
            LocalDate.parse(dateString)
        } catch (e: DateTimeParseException) {
            throw ApiException("Date must be in YYYY-MM-DD format", HttpStatusCode.BadRequest)
        }
    }
}