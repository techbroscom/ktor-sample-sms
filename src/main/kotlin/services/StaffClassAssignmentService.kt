package com.example.services

import com.example.database.tables.StaffClassRole
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.StaffClassAssignmentRepository
import io.ktor.http.*

class StaffClassAssignmentService(
    private val staffClassAssignmentRepository: StaffClassAssignmentRepository,
    private val userService: UserService,
    private val classService: ClassService,
    private val academicYearService: AcademicYearService
) {

    suspend fun createStaffClassAssignment(request: CreateStaffClassAssignmentRequest): StaffClassAssignmentDto {
        validateStaffClassAssignmentRequest(request.staffId, request.classId, request.academicYearId, request.role)

        // Validate that referenced entities exist
        userService.getUserById(request.staffId) // Check if staff exists
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate
        val isDuplicate = staffClassAssignmentRepository.checkDuplicate(
            request.staffId,
            request.classId,
            request.academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "This staff member is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val assignmentId = staffClassAssignmentRepository.create(request)
        return getStaffClassAssignmentById(assignmentId)
    }

    suspend fun bulkCreateStaffClassAssignments(request: BulkCreateStaffClassAssignmentRequest): List<StaffClassAssignmentDto> {
        validateUUID(request.staffId, "Staff ID")
        validateUUID(request.academicYearId, "Academic Year ID")

        if (request.classIds.isEmpty()) {
            throw ApiException("Class IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        if (request.role != null) {
            validateRole(request.role)
        }

        // Validate that referenced entities exist
        userService.getUserById(request.staffId)
        academicYearService.getAcademicYearById(request.academicYearId)

        val createRequests = mutableListOf<CreateStaffClassAssignmentRequest>()

        for (classId in request.classIds) {
            validateUUID(classId, "Class ID")
            classService.getClassById(classId)

            // Check for duplicate
            val isDuplicate = staffClassAssignmentRepository.checkDuplicate(
                request.staffId,
                classId,
                request.academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(CreateStaffClassAssignmentRequest(
                    staffId = request.staffId,
                    classId = classId,
                    academicYearId = request.academicYearId,
                    role = request.role
                ))
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException("This staff member is already assigned to all specified classes", HttpStatusCode.Conflict)
        }

        val assignmentIds = staffClassAssignmentRepository.bulkCreate(createRequests)
        return assignmentIds.map { getStaffClassAssignmentById(it) }
    }

    suspend fun getStaffClassAssignmentById(id: String): StaffClassAssignmentDto {
        validateUUID(id, "Staff Class Assignment ID")
        return staffClassAssignmentRepository.findById(id)
            ?: throw ApiException("Staff Class assignment not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllStaffClassAssignments(): List<StaffClassAssignmentDto> {
        return staffClassAssignmentRepository.findAll()
    }

    suspend fun updateStaffClassAssignment(id: String, request: UpdateStaffClassAssignmentRequest): StaffClassAssignmentDto {
        validateUUID(id, "Staff Class Assignment ID")
        validateStaffClassAssignmentRequest(request.staffId, request.classId, request.academicYearId, request.role)

        // Validate that referenced entities exist
        userService.getUserById(request.staffId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = staffClassAssignmentRepository.checkDuplicate(
            request.staffId,
            request.classId,
            request.academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "This staff member is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = staffClassAssignmentRepository.update(id, request)
        if (!updated) {
            throw ApiException("Staff Class assignment not found", HttpStatusCode.NotFound)
        }

        return getStaffClassAssignmentById(id)
    }

    suspend fun deleteStaffClassAssignment(id: String) {
        validateUUID(id, "Staff Class Assignment ID")
        val deleted = staffClassAssignmentRepository.delete(id)
        if (!deleted) {
            throw ApiException("Staff Class assignment not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getClassesByStaff(staffId: String): List<StaffClassAssignmentDto> {
        validateUUID(staffId, "Staff ID")
        // Validate staff exists
        userService.getUserById(staffId)
        return staffClassAssignmentRepository.findByStaffId(staffId)
    }

    suspend fun getStaffByClass(classId: String): List<StaffClassAssignmentDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return staffClassAssignmentRepository.findByClassId(classId)
    }

    suspend fun getStaffClassAssignmentsByAcademicYear(academicYearId: String): List<StaffClassAssignmentDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return staffClassAssignmentRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getClassesByStaffAndAcademicYear(staffId: String, academicYearId: String): List<StaffClassAssignmentDto> {
        validateUUID(staffId, "Staff ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        userService.getUserById(staffId)
        academicYearService.getAcademicYearById(academicYearId)

        return staffClassAssignmentRepository.findByStaffAndAcademicYear(staffId, academicYearId)
    }

    suspend fun getStaffByRole(role: String): List<StaffClassAssignmentDto> {
        validateRole(role)
        return staffClassAssignmentRepository.findByRole(role)
    }

    suspend fun getStaffWithClasses(academicYearId: String): List<StaffWithClassesDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return staffClassAssignmentRepository.getStaffWithClasses(academicYearId)
    }

    suspend fun getClassesWithStaff(academicYearId: String): List<ClassWithStaffDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return staffClassAssignmentRepository.getClassesWithStaff(academicYearId)
    }

    suspend fun removeAllClassesFromStaff(staffId: String): Int {
        validateUUID(staffId, "Staff ID")
        // Validate staff exists
        userService.getUserById(staffId)
        return staffClassAssignmentRepository.deleteByStaffId(staffId)
    }

    suspend fun removeAllStaffFromClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return staffClassAssignmentRepository.deleteByClassId(classId)
    }

    private fun validateStaffClassAssignmentRequest(staffId: String, classId: String, academicYearId: String, role: String?) {
        validateUUID(staffId, "Staff ID")
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")
        if (role != null) {
            validateRole(role)
        }
    }

    private fun validateRole(role: String) {
        try {
            StaffClassRole.valueOf(role)
        } catch (e: IllegalArgumentException) {
            throw ApiException(
                "Invalid role. Must be one of: ${StaffClassRole.values().joinToString(", ")}",
                HttpStatusCode.BadRequest
            )
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