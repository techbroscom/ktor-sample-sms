package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.StudentAssignmentRepository
import io.ktor.http.*

class StudentAssignmentService(
    private val studentAssignmentRepository: StudentAssignmentRepository,
    private val userService: UserService,
    private val classService: ClassService,
    private val academicYearService: AcademicYearService
) {

    suspend fun createStudentAssignment(request: CreateStudentAssignmentRequest): StudentAssignmentDto {
        validateStudentAssignmentRequest(request.studentId, request.classId, request.academicYearId)

        // Validate that referenced entities exist
        userService.getUserById(request.studentId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate
        val isDuplicate = studentAssignmentRepository.checkDuplicate(
            request.studentId,
            request.classId,
            request.academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "This student is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val assignmentId = studentAssignmentRepository.create(request)
        return getStudentAssignmentById(assignmentId)
    }

    suspend fun bulkCreateStudentAssignments(request: BulkCreateStudentAssignmentRequest): List<StudentAssignmentDto> {
        validateUUID(request.classId, "Class ID")
        validateUUID(request.academicYearId, "Academic Year ID")

        if (request.studentIds.isEmpty()) {
            throw ApiException("Student IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        val createRequests = mutableListOf<CreateStudentAssignmentRequest>()

        for (studentId in request.studentIds) {
            validateUUID(studentId, "Student ID")
            userService.getUserById(studentId)

            // Check for duplicate
            val isDuplicate = studentAssignmentRepository.checkDuplicate(
                studentId,
                request.classId,
                request.academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(CreateStudentAssignmentRequest(
                    studentId = studentId,
                    classId = request.classId,
                    academicYearId = request.academicYearId
                ))
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException("All students are already assigned to this class", HttpStatusCode.Conflict)
        }

        val assignmentIds = studentAssignmentRepository.bulkCreate(createRequests)
        return assignmentIds.map { getStudentAssignmentById(it) }
    }

    suspend fun getStudentAssignmentById(id: String): StudentAssignmentDto {
        validateUUID(id, "Student Assignment ID")
        return studentAssignmentRepository.findById(id)
            ?: throw ApiException("Student assignment not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllStudentAssignments(): List<StudentAssignmentDto> {
        return studentAssignmentRepository.findAll()
    }

    suspend fun updateStudentAssignment(id: String, request: UpdateStudentAssignmentRequest): StudentAssignmentDto {
        validateUUID(id, "Student Assignment ID")
        validateStudentAssignmentRequest(request.studentId, request.classId, request.academicYearId)

        // Validate that referenced entities exist
        userService.getUserById(request.studentId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = studentAssignmentRepository.checkDuplicate(
            request.studentId,
            request.classId,
            request.academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "This student is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = studentAssignmentRepository.update(id, request)
        if (!updated) {
            throw ApiException("Student assignment not found", HttpStatusCode.NotFound)
        }

        return getStudentAssignmentById(id)
    }

    suspend fun deleteStudentAssignment(id: String) {
        validateUUID(id, "Student Assignment ID")
        val deleted = studentAssignmentRepository.delete(id)
        if (!deleted) {
            throw ApiException("Student assignment not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getStudentAssignments(studentId: String): List<StudentAssignmentDto> {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        userService.getUserById(studentId)
        return studentAssignmentRepository.findByStudentId(studentId)
    }

    suspend fun getClassStudents(classId: String): List<StudentAssignmentDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return studentAssignmentRepository.findByClassId(classId)
    }

    suspend fun getStudentAssignmentsByAcademicYear(academicYearId: String): List<StudentAssignmentDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return studentAssignmentRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getClassStudentsByAcademicYear(classId: String, academicYearId: String): List<StudentAssignmentDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return studentAssignmentRepository.findByClassAndAcademicYear(classId, academicYearId)
    }

    suspend fun getStudentCurrentClass(studentId: String, academicYearId: String): StudentAssignmentDto? {
        validateUUID(studentId, "Student ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        userService.getUserById(studentId)
        academicYearService.getAcademicYearById(academicYearId)

        return studentAssignmentRepository.findByStudentAndAcademicYear(studentId, academicYearId)
    }

    suspend fun getClassesWithStudents(academicYearId: String): List<ClassWithStudentsDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return studentAssignmentRepository.getClassesWithStudents(academicYearId)
    }

    suspend fun getStudentsWithClasses(academicYearId: String): List<StudentWithClassDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return studentAssignmentRepository.getStudentsWithClasses(academicYearId)
    }

    suspend fun transferStudents(request: BulkTransferStudentsRequest): Int {
        validateUUID(request.fromClassId, "From Class ID")
        validateUUID(request.toClassId, "To Class ID")
        validateUUID(request.academicYearId, "Academic Year ID")

        if (request.studentIds.isEmpty()) {
            throw ApiException("Student IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        if (request.fromClassId == request.toClassId) {
            throw ApiException("Source and destination classes cannot be the same", HttpStatusCode.BadRequest)
        }

        // Validate entities exist
        classService.getClassById(request.fromClassId)
        classService.getClassById(request.toClassId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Validate students exist
        request.studentIds.forEach { studentId ->
            validateUUID(studentId, "Student ID")
            userService.getUserById(studentId)
        }

        return studentAssignmentRepository.transferStudents(
            request.studentIds,
            request.fromClassId,
            request.toClassId,
            request.academicYearId
        )
    }

    suspend fun removeAllStudentsFromClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return studentAssignmentRepository.deleteByClassId(classId)
    }

    suspend fun removeStudentFromAllClasses(studentId: String): Int {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        userService.getUserById(studentId)
        return studentAssignmentRepository.deleteByStudentId(studentId)
    }

    suspend fun getStudentCountByClass(academicYearId: String): List<Pair<String, Int>> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return studentAssignmentRepository.getStudentCountByClass(academicYearId)
    }

    private fun validateStudentAssignmentRequest(studentId: String, classId: String, academicYearId: String) {
        validateUUID(studentId, "Student ID")
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("$fieldName must be a valid UUID", HttpStatusCode.BadRequest)
        }
    }

    suspend fun getStudentAssignmentSummary(studentId: String): StudentAssignmentSummaryDto {
        validateUUID(studentId, "Student ID")
        userService.getUserById(studentId) // Validate student exists

        val assignments = studentAssignmentRepository.findByStudentId(studentId)

        val assignmentsByYear = assignments.groupBy { it.academicYearName ?: "Unknown" }
            .mapValues { it.value.size }

        val assignmentsByClass = assignments.groupBy { "${it.className} - ${it.sectionName}" }
            .mapValues { it.value.size }

        return StudentAssignmentSummaryDto(
            totalAssignments = assignments.size,
            assignmentsByAcademicYear = assignmentsByYear,
            assignmentsByClass = assignmentsByClass,
            mostRecentAssignment = assignments.firstOrNull()
        )
    }

    suspend fun getAcademicYearSummary(academicYearId: String): AcademicYearSummaryDto {
        validateUUID(academicYearId, "Academic Year ID")
        val academicYear = academicYearService.getAcademicYearById(academicYearId)

        val assignments = studentAssignmentRepository.findByAcademicYear(academicYearId)
        val classCounts = studentAssignmentRepository.getStudentCountByClass(academicYearId)

        val uniqueStudents = assignments.map { it.studentId }.toSet().size
        val uniqueClasses = assignments.map { it.classId }.toSet().size

        val classCapacities = classCounts.map { (className, count) ->
            ClassCapacityDto(
                classId = assignments.find { "${it.className} - ${it.sectionName}" == className }?.classId ?: "",
                className = className.substringBefore(" - "),
                sectionName = className.substringAfter(" - "),
                currentStudentCount = count,
                maxCapacity = null, // Would need to be fetched from class definition
                availableSpots = null
            )
        }

        return AcademicYearSummaryDto(
            academicYearId = academicYearId,
            academicYearName = academicYear.year,
            totalStudents = uniqueStudents,
            totalClasses = uniqueClasses,
            averageStudentsPerClass = if (uniqueClasses > 0) uniqueStudents.toDouble() / uniqueClasses else 0.0,
            classesWithStudents = classCapacities
        )
    }

    suspend fun validateStudentAssignment(studentId: String, classId: String, academicYearId: String): Boolean {
        return try {
            validateStudentAssignmentRequest(studentId, classId, academicYearId)
            userService.getUserById(studentId)
            classService.getClassById(classId)
            academicYearService.getAcademicYearById(academicYearId)
            true
        } catch (e: Exception) {
            false
        }
    }

    // Add these implementations to your StudentAssignmentService class

    suspend fun getStudentsNotInClass(classId: String, academicYearId: String): List<StudentDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate that referenced entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return studentAssignmentRepository.findStudentsNotInClass(classId, academicYearId)
    }

    suspend fun getUnassignedStudents(academicYearId: String): List<StudentDto> {
        validateUUID(academicYearId, "Academic Year ID")

        // Validate that academic year exists
        academicYearService.getAcademicYearById(academicYearId)

        return studentAssignmentRepository.findUnassignedStudents(academicYearId)
    }

    suspend fun getAssignmentHistory(studentId: String): List<StudentAssignmentDto> {
        validateUUID(studentId, "Student ID")

        // Validate that student exists
        userService.getUserById(studentId)

        return studentAssignmentRepository.getAssignmentHistory(studentId)
    }
}