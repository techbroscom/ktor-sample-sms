package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.StaffSubjectAssignmentRepository
import io.ktor.http.*

class StaffSubjectAssignmentService(
    private val staffSubjectAssignmentRepository: StaffSubjectAssignmentRepository,
    private val userService: UserService,
    private val classService: ClassService,
    private val classSubjectService: ClassSubjectService,
    private val academicYearService: AcademicYearService
) {

    suspend fun createStaffSubjectAssignment(request: CreateStaffSubjectAssignmentRequest): StaffSubjectAssignmentDto {
        validateStaffSubjectAssignmentRequest(
            request.staffId,
            request.classSubjectId,
            request.classId,
            request.academicYearId
        )

        // Validate that referenced entities exist
        userService.getUserById(request.staffId) // Check if staff exists
        classSubjectService.getClassSubjectById(request.classSubjectId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate
        val isDuplicate = staffSubjectAssignmentRepository.checkDuplicate(
            request.staffId,
            request.classSubjectId,
            request.classId,
            request.academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "This staff member is already assigned to this subject in this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val assignmentId = staffSubjectAssignmentRepository.create(request)
        return getStaffSubjectAssignmentById(assignmentId)
    }

    suspend fun bulkCreateStaffSubjectAssignments(request: BulkCreateStaffSubjectAssignmentRequest): List<StaffSubjectAssignmentDto> {
        validateUUID(request.staffId, "Staff ID")
        validateUUID(request.classId, "Class ID")
        validateUUID(request.academicYearId, "Academic Year ID")

        if (request.classSubjectIds.isEmpty()) {
            throw ApiException("Class Subject IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        userService.getUserById(request.staffId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        val createRequests = mutableListOf<CreateStaffSubjectAssignmentRequest>()

        for (classSubjectId in request.classSubjectIds) {
            validateUUID(classSubjectId, "Class Subject ID")
            classSubjectService.getClassSubjectById(classSubjectId)

            // Check for duplicate
            val isDuplicate = staffSubjectAssignmentRepository.checkDuplicate(
                request.staffId,
                classSubjectId,
                request.classId,
                request.academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(
                    CreateStaffSubjectAssignmentRequest(
                        staffId = request.staffId,
                        classSubjectId = classSubjectId,
                        classId = request.classId,
                        academicYearId = request.academicYearId
                    )
                )
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException(
                "This staff member is already assigned to all specified subjects",
                HttpStatusCode.Conflict
            )
        }

        val assignmentIds = staffSubjectAssignmentRepository.bulkCreate(createRequests)
        return assignmentIds.map { getStaffSubjectAssignmentById(it) }
    }

    suspend fun getStaffSubjectAssignmentById(id: String): StaffSubjectAssignmentDto {
        validateUUID(id, "Staff Subject Assignment ID")
        return staffSubjectAssignmentRepository.findById(id)
            ?: throw ApiException("Staff Subject assignment not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllStaffSubjectAssignments(): List<StaffSubjectAssignmentDto> {
        return staffSubjectAssignmentRepository.findAll()
    }

    suspend fun updateStaffSubjectAssignment(
        id: String,
        request: UpdateStaffSubjectAssignmentRequest
    ): StaffSubjectAssignmentDto {
        validateUUID(id, "Staff Subject Assignment ID")
        validateStaffSubjectAssignmentRequest(
            request.staffId,
            request.classSubjectId,
            request.classId,
            request.academicYearId
        )

        // Validate that referenced entities exist
        userService.getUserById(request.staffId)
        classSubjectService.getClassSubjectById(request.classSubjectId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = staffSubjectAssignmentRepository.checkDuplicate(
            request.staffId,
            request.classSubjectId,
            request.classId,
            request.academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "This staff member is already assigned to this subject in this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = staffSubjectAssignmentRepository.update(id, request)
        if (!updated) {
            throw ApiException("Staff Subject assignment not found", HttpStatusCode.NotFound)
        }

        return getStaffSubjectAssignmentById(id)
    }

    suspend fun deleteStaffSubjectAssignment(id: String) {
        validateUUID(id, "Staff Subject Assignment ID")
        val deleted = staffSubjectAssignmentRepository.delete(id)
        if (!deleted) {
            throw ApiException("Staff Subject assignment not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getSubjectsByStaff(staffId: String): List<StaffSubjectAssignmentDto> {
        validateUUID(staffId, "Staff ID")
        // Validate staff exists
        userService.getUserById(staffId)
        return staffSubjectAssignmentRepository.findByStaffId(staffId)
    }

    suspend fun getStaffByClass(classId: String): List<StaffSubjectAssignmentDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return staffSubjectAssignmentRepository.findByClassId(classId)
    }

    suspend fun getStaffByClassSubject(classSubjectId: String): List<StaffSubjectAssignmentDto> {
        validateUUID(classSubjectId, "Class Subject ID")
        // Validate class subject exists
        classSubjectService.getClassSubjectById(classSubjectId)
        return staffSubjectAssignmentRepository.findByClassSubjectId(classSubjectId)
    }

    suspend fun getStaffSubjectAssignmentsByAcademicYear(academicYearId: String): List<StaffSubjectAssignmentDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return staffSubjectAssignmentRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getSubjectsByStaffAndAcademicYear(
        staffId: String,
        academicYearId: String
    ): List<StaffSubjectAssignmentDto> {
        validateUUID(staffId, "Staff ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        userService.getUserById(staffId)
        academicYearService.getAcademicYearById(academicYearId)

        return staffSubjectAssignmentRepository.findByStaffAndAcademicYear(staffId, academicYearId)
    }

    suspend fun getStaffByClassAndAcademicYear(
        classId: String,
        academicYearId: String
    ): List<StaffSubjectAssignmentDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return staffSubjectAssignmentRepository.findByClassAndAcademicYear(classId, academicYearId)
    }

    suspend fun getStaffWithSubjects(academicYearId: String): List<StaffWithSubjectsDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return staffSubjectAssignmentRepository.getStaffWithSubjects(academicYearId)
    }

    suspend fun getClassesWithSubjectStaff(academicYearId: String): List<ClassWithSubjectStaffDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return staffSubjectAssignmentRepository.getClassesWithSubjectStaff(academicYearId)
    }

    suspend fun deleteStaffSubjectAssignmentsByStaff(staffId: String): Int {
        validateUUID(staffId, "Staff ID")
        // Validate staff exists
        userService.getUserById(staffId)
        return staffSubjectAssignmentRepository.deleteByStaffId(staffId)
    }

    suspend fun deleteStaffSubjectAssignmentsByClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return staffSubjectAssignmentRepository.deleteByClassId(classId)
    }

    suspend fun deleteStaffSubjectAssignmentsByClassSubject(classSubjectId: String): Int {
        validateUUID(classSubjectId, "Class Subject ID")
        // Validate class subject exists
        classSubjectService.getClassSubjectById(classSubjectId)
        return staffSubjectAssignmentRepository.deleteByClassSubjectId(classSubjectId)
    }

    suspend fun deleteStaffSubjectAssignmentsByAcademicYear(academicYearId: String): Int {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return staffSubjectAssignmentRepository.deleteByAcademicYear(academicYearId)
    }

    private fun validateStaffSubjectAssignmentRequest(
        staffId: String,
        classSubjectId: String,
        classId: String,
        academicYearId: String
    ) {
        validateUUID(staffId, "Staff ID")
        validateUUID(classSubjectId, "Class Subject ID")
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid $fieldName format", HttpStatusCode.BadRequest)
        }
    }
}