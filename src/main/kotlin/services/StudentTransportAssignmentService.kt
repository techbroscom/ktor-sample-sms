package com.example.services

import com.example.database.tables.UserRole
import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateStudentTransportAssignmentRequest
import com.example.models.dto.CreateStudentTransportAssignmentRequest
import com.example.models.dto.StudentTransportAssignmentDto
import com.example.models.dto.UpdateStudentTransportAssignmentRequest
import com.example.repositories.StudentTransportAssignmentRepository
import com.example.repositories.UserRepository
import com.example.repositories.AcademicYearRepository
import com.example.repositories.TransportRouteRepository
import com.example.repositories.TransportStopRepository
import io.ktor.http.*
import java.time.LocalDate
import java.util.*

class StudentTransportAssignmentService(
    private val studentTransportAssignmentRepository: StudentTransportAssignmentRepository,
    private val userRepository: UserRepository,
    private val academicYearRepository: AcademicYearRepository,
    private val transportRouteRepository: TransportRouteRepository,
    private val transportStopRepository: TransportStopRepository
) {

    suspend fun createStudentTransportAssignment(request: CreateStudentTransportAssignmentRequest): StudentTransportAssignmentDto {
        validateCreateRequest(request)

        val studentId = UUID.fromString(request.studentId)
        val academicYearId = UUID.fromString(request.academicYearId)
        val routeId = UUID.fromString(request.routeId)
        val stopId = UUID.fromString(request.stopId)

        // Validate all referenced entities exist
        validateReferencedEntities(studentId, academicYearId, routeId, stopId)

        // Check if student already has an assignment for this academic year
        if (studentTransportAssignmentRepository.assignmentExistsForStudentInAcademicYear(studentId, academicYearId)) {
            throw ApiException("Student already has a transport assignment for this academic year", HttpStatusCode.Conflict)
        }

        val assignmentId = studentTransportAssignmentRepository.create(request)
        return getStudentTransportAssignmentById(assignmentId)
    }

    suspend fun bulkCreateStudentTransportAssignments(request: BulkCreateStudentTransportAssignmentRequest): List<StudentTransportAssignmentDto> {
        validateBulkCreateRequest(request)

        val academicYearId = UUID.fromString(request.academicYearId)
        val routeId = UUID.fromString(request.routeId)
        val stopId = UUID.fromString(request.stopId)

        // Validate academic year, route, and stop exist
        academicYearRepository.findById(academicYearId)
            ?: throw ApiException("Academic year not found", HttpStatusCode.NotFound)

        transportRouteRepository.findById(routeId)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        transportStopRepository.findById(stopId)
            ?: throw ApiException("Transport stop not found", HttpStatusCode.NotFound)

        // Validate all students exist and are students
        val studentIds = request.studentIds.map { UUID.fromString(it) }
        for (studentId in studentIds) {
            val user = userRepository.findById(studentId)
                ?: throw ApiException("Student not found: $studentId", HttpStatusCode.NotFound)

            if (user.role != UserRole.STUDENT.name) {
                throw ApiException("User is not a student: $studentId", HttpStatusCode.BadRequest)
            }

            // Check if student already has an assignment for this academic year
            if (studentTransportAssignmentRepository.assignmentExistsForStudentInAcademicYear(studentId, academicYearId)) {
                throw ApiException("Student $studentId already has a transport assignment for this academic year", HttpStatusCode.Conflict)
            }
        }

        // Create individual requests for each student
        val createRequests = request.studentIds.map { studentId ->
            CreateStudentTransportAssignmentRequest(
                studentId = studentId,
                academicYearId = request.academicYearId,
                routeId = request.routeId,
                stopId = request.stopId,
                startDate = request.startDate,
                endDate = request.endDate,
                isActive = request.isActive
            )
        }

        val assignmentIds = studentTransportAssignmentRepository.bulkCreate(createRequests)
        return assignmentIds.mapNotNull { getStudentTransportAssignmentById(it) }
    }

    suspend fun getStudentTransportAssignmentById(id: UUID): StudentTransportAssignmentDto {
        return studentTransportAssignmentRepository.findById(id)
            ?: throw ApiException("Student transport assignment not found", HttpStatusCode.NotFound)
    }

    suspend fun getStudentTransportAssignmentById(id: String): StudentTransportAssignmentDto {
        val uuid = try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid assignment ID format", HttpStatusCode.BadRequest)
        }
        return getStudentTransportAssignmentById(uuid)
    }

    suspend fun getAllStudentTransportAssignments(): List<StudentTransportAssignmentDto> {
        return studentTransportAssignmentRepository.findAll()
    }

    suspend fun getStudentTransportAssignmentsByStudentId(studentId: String): List<StudentTransportAssignmentDto> {
        val uuid = validateAndParseUUID(studentId, "Invalid student ID format")

        // Verify student exists
        val user = userRepository.findById(uuid)
            ?: throw ApiException("Student not found", HttpStatusCode.NotFound)

        if (user.role != UserRole.STUDENT.name) {
            throw ApiException("User is not a student", HttpStatusCode.BadRequest)
        }

        return studentTransportAssignmentRepository.findByStudentId(uuid)
    }

    suspend fun getStudentTransportAssignmentsByAcademicYearId(academicYearId: String): List<StudentTransportAssignmentDto> {
        val uuid = validateAndParseUUID(academicYearId, "Invalid academic year ID format")

        // Verify academic year exists
        academicYearRepository.findById(uuid)
            ?: throw ApiException("Academic year not found", HttpStatusCode.NotFound)

        return studentTransportAssignmentRepository.findByAcademicYearId(uuid)
    }

    suspend fun getStudentTransportAssignmentsByRouteId(routeId: String): List<StudentTransportAssignmentDto> {
        val uuid = validateAndParseUUID(routeId, "Invalid route ID format")

        // Verify route exists
        transportRouteRepository.findById(uuid)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        return studentTransportAssignmentRepository.findByRouteId(uuid)
    }

    suspend fun getStudentTransportAssignmentsByStopId(stopId: String): List<StudentTransportAssignmentDto> {
        val uuid = validateAndParseUUID(stopId, "Invalid stop ID format")

        // Verify stop exists
        transportStopRepository.findById(uuid)
            ?: throw ApiException("Transport stop not found", HttpStatusCode.NotFound)

        return studentTransportAssignmentRepository.findByStopId(uuid)
    }

    suspend fun getActiveStudentTransportAssignments(): List<StudentTransportAssignmentDto> {
        return studentTransportAssignmentRepository.findActive()
    }

    suspend fun getActiveStudentTransportAssignmentsByAcademicYear(academicYearId: String): List<StudentTransportAssignmentDto> {
        val uuid = validateAndParseUUID(academicYearId, "Invalid academic year ID format")

        // Verify academic year exists
        academicYearRepository.findById(uuid)
            ?: throw ApiException("Academic year not found", HttpStatusCode.NotFound)

        return studentTransportAssignmentRepository.findActiveByAcademicYear(uuid)
    }

    suspend fun updateStudentTransportAssignment(id: String, request: UpdateStudentTransportAssignmentRequest): StudentTransportAssignmentDto {
        val uuid = validateAndParseUUID(id, "Invalid assignment ID format")

        validateUpdateRequest(request)

        val studentId = UUID.fromString(request.studentId)
        val academicYearId = UUID.fromString(request.academicYearId)
        val routeId = UUID.fromString(request.routeId)
        val stopId = UUID.fromString(request.stopId)

        // Validate all referenced entities exist
        validateReferencedEntities(studentId, academicYearId, routeId, stopId)

        // Check if student already has another assignment for this academic year
        if (studentTransportAssignmentRepository.assignmentExistsForStudentInAcademicYearExcludingId(studentId, academicYearId, uuid)) {
            throw ApiException("Student already has another transport assignment for this academic year", HttpStatusCode.Conflict)
        }

        val updated = studentTransportAssignmentRepository.update(uuid, request)
        if (!updated) {
            throw ApiException("Student transport assignment not found", HttpStatusCode.NotFound)
        }

        return getStudentTransportAssignmentById(uuid)
    }

    suspend fun deleteStudentTransportAssignment(id: String) {
        val uuid = validateAndParseUUID(id, "Invalid assignment ID format")

        val deleted = studentTransportAssignmentRepository.delete(uuid)
        if (!deleted) {
            throw ApiException("Student transport assignment not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun toggleStudentTransportAssignmentStatus(id: String): StudentTransportAssignmentDto {
        val uuid = validateAndParseUUID(id, "Invalid assignment ID format")

        val updated = studentTransportAssignmentRepository.toggleActiveStatus(uuid)
        if (!updated) {
            throw ApiException("Student transport assignment not found", HttpStatusCode.NotFound)
        }

        return getStudentTransportAssignmentById(uuid)
    }

    private suspend fun validateReferencedEntities(studentId: UUID, academicYearId: UUID, routeId: UUID, stopId: UUID) {
        // Verify student exists and is a student
        val user = userRepository.findById(studentId)
            ?: throw ApiException("Student not found", HttpStatusCode.NotFound)

        if (user.role != UserRole.STUDENT.name) {
            throw ApiException("User is not a student", HttpStatusCode.BadRequest)
        }

        // Verify academic year exists
        academicYearRepository.findById(academicYearId)
            ?: throw ApiException("Academic year not found", HttpStatusCode.NotFound)

        // Verify route exists
        transportRouteRepository.findById(routeId)
            ?: throw ApiException("Transport route not found", HttpStatusCode.NotFound)

        // Verify stop exists and belongs to the route
        val stop = transportStopRepository.findById(stopId)
            ?: throw ApiException("Transport stop not found", HttpStatusCode.NotFound)

        if (stop.routeId != routeId.toString()) {
            throw ApiException("Transport stop does not belong to the specified route", HttpStatusCode.BadRequest)
        }
    }

    private fun validateAndParseUUID(id: String, errorMessage: String): UUID {
        return try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException(errorMessage, HttpStatusCode.BadRequest)
        }
    }

    private fun validateCreateRequest(request: CreateStudentTransportAssignmentRequest) {
        when {
            request.studentId.isBlank() -> throw ApiException("Student ID cannot be empty", HttpStatusCode.BadRequest)
            request.academicYearId.isBlank() -> throw ApiException("Academic year ID cannot be empty", HttpStatusCode.BadRequest)
            request.routeId.isBlank() -> throw ApiException("Route ID cannot be empty", HttpStatusCode.BadRequest)
            request.stopId.isBlank() -> throw ApiException("Stop ID cannot be empty", HttpStatusCode.BadRequest)
            request.startDate.isBlank() -> throw ApiException("Start date cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate UUID formats
        try {
            UUID.fromString(request.studentId)
            UUID.fromString(request.academicYearId)
            UUID.fromString(request.routeId)
            UUID.fromString(request.stopId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid UUID format in request", HttpStatusCode.BadRequest)
        }

        // Validate date formats
        try {
            LocalDate.parse(request.startDate)
            request.endDate?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
            throw ApiException("Invalid date format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
        }

        // Validate date logic
        if (request.endDate != null) {
            val startDate = LocalDate.parse(request.startDate)
            val endDate = LocalDate.parse(request.endDate)
            if (endDate.isBefore(startDate)) {
                throw ApiException("End date cannot be before start date", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun validateUpdateRequest(request: UpdateStudentTransportAssignmentRequest) {
        when {
            request.studentId.isBlank() -> throw ApiException("Student ID cannot be empty", HttpStatusCode.BadRequest)
            request.academicYearId.isBlank() -> throw ApiException("Academic year ID cannot be empty", HttpStatusCode.BadRequest)
            request.routeId.isBlank() -> throw ApiException("Route ID cannot be empty", HttpStatusCode.BadRequest)
            request.stopId.isBlank() -> throw ApiException("Stop ID cannot be empty", HttpStatusCode.BadRequest)
            request.startDate.isBlank() -> throw ApiException("Start date cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate UUID formats
        try {
            UUID.fromString(request.studentId)
            UUID.fromString(request.academicYearId)
            UUID.fromString(request.routeId)
            UUID.fromString(request.stopId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid UUID format in request", HttpStatusCode.BadRequest)
        }

        // Validate date formats
        try {
            LocalDate.parse(request.startDate)
            request.endDate?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
            throw ApiException("Invalid date format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
        }

        // Validate date logic
        if (request.endDate != null) {
            val startDate = LocalDate.parse(request.startDate)
            val endDate = LocalDate.parse(request.endDate)
            if (endDate.isBefore(startDate)) {
                throw ApiException("End date cannot be before start date", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun validateBulkCreateRequest(request: BulkCreateStudentTransportAssignmentRequest) {
        when {
            request.studentIds.isEmpty() -> throw ApiException("Student IDs list cannot be empty", HttpStatusCode.BadRequest)
            request.academicYearId.isBlank() -> throw ApiException("Academic year ID cannot be empty", HttpStatusCode.BadRequest)
            request.routeId.isBlank() -> throw ApiException("Route ID cannot be empty", HttpStatusCode.BadRequest)
            request.stopId.isBlank() -> throw ApiException("Stop ID cannot be empty", HttpStatusCode.BadRequest)
            request.startDate.isBlank() -> throw ApiException("Start date cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate UUID formats
        try {
            UUID.fromString(request.academicYearId)
            UUID.fromString(request.routeId)
            UUID.fromString(request.stopId)
            request.studentIds.forEach { UUID.fromString(it) }
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid UUID format in request", HttpStatusCode.BadRequest)
        }

        // Validate date formats
        try {
            LocalDate.parse(request.startDate)
            request.endDate?.let { LocalDate.parse(it) }
        } catch (e: Exception) {
            throw ApiException("Invalid date format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
        }

        // Validate date logic
        if (request.endDate != null) {
            val startDate = LocalDate.parse(request.startDate)
            val endDate = LocalDate.parse(request.endDate)
            if (endDate.isBefore(startDate)) {
                throw ApiException("End date cannot be before start date", HttpStatusCode.BadRequest)
            }
        }
    }
}