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
        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateStudentAssignmentRequest(request.studentId, request.classId, academicYearId)

        // Validate that referenced entities exist
        userService.getUserById(request.studentId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(academicYearId)

        // Check for duplicate
        val isDuplicate = studentAssignmentRepository.checkDuplicate(
            request.studentId,
            request.classId,
            academicYearId
        )
        if (isDuplicate) {
            throw ApiException(
                "This student is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val assignmentId = studentAssignmentRepository.create(request.copy(academicYearId = academicYearId))
        return getStudentAssignmentById(assignmentId)
    }

    suspend fun bulkCreateStudentAssignments(request: BulkCreateStudentAssignmentRequest): List<StudentAssignmentDto> {
        validateUUID(request.classId, "Class ID")

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        if (request.studentIds.isEmpty()) {
            throw ApiException("Student IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(academicYearId)

        val createRequests = mutableListOf<CreateStudentAssignmentRequest>()

        for (studentId in request.studentIds) {
            validateUUID(studentId, "Student ID")
            userService.getUserById(studentId)

            // Check for duplicate
            val isDuplicate = studentAssignmentRepository.checkDuplicate(
                studentId,
                request.classId,
                academicYearId
            )
            if (!isDuplicate) {
                createRequests.add(CreateStudentAssignmentRequest(
                    studentId = studentId,
                    classId = request.classId,
                    academicYearId = academicYearId
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

    suspend fun getStudentAssignmentsForActiveYear(): List<StudentAssignmentDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.findByAcademicYear(activeAcademicYear.id)
    }

    suspend fun updateStudentAssignment(id: String, request: UpdateStudentAssignmentRequest): StudentAssignmentDto {
        validateUUID(id, "Student Assignment ID")

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateStudentAssignmentRequest(request.studentId, request.classId, academicYearId)

        // Validate that referenced entities exist
        userService.getUserById(request.studentId)
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = studentAssignmentRepository.checkDuplicate(
            request.studentId,
            request.classId,
            academicYearId,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "This student is already assigned to this class for the specified academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = studentAssignmentRepository.update(id, request.copy(academicYearId = academicYearId))
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

    suspend fun getStudentAssignmentsForActiveYear(studentId: String): List<StudentAssignmentDto> {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        userService.getUserById(studentId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.findByStudentAndAcademicYear(studentId, activeAcademicYear.id)?.let { listOf(it) } ?: emptyList()
    }

    suspend fun getClassStudents(classId: String): List<StudentAssignmentDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return studentAssignmentRepository.findByClassId(classId)
    }

    suspend fun getClassStudentsForActiveYear(classId: String): List<StudentAssignmentDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.findByClassAndAcademicYear(classId, activeAcademicYear.id)
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

    suspend fun getStudentCurrentClass(studentId: String, academicYearId: String? = null): StudentAssignmentDto? {
        validateUUID(studentId, "Student ID")

        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        // Validate entities exist
        userService.getUserById(studentId)
        academicYearService.getAcademicYearById(finalAcademicYearId)

        return studentAssignmentRepository.findByStudentAndAcademicYear(studentId, finalAcademicYearId)
    }

    suspend fun getClassesWithStudents(academicYearId: String? = null): List<ClassWithStudentsDto> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(finalAcademicYearId)
        return studentAssignmentRepository.getClassesWithStudents(finalAcademicYearId)
    }

    suspend fun getClassesWithStudentsForActiveYear(): List<ClassWithStudentsDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.getClassesWithStudents(activeAcademicYear.id)
    }

    suspend fun getStudentsWithClasses(academicYearId: String? = null): List<StudentWithClassDto> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(finalAcademicYearId)
        return studentAssignmentRepository.getStudentsWithClasses(finalAcademicYearId)
    }

    suspend fun getStudentsWithClassesForActiveYear(): List<StudentWithClassDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.getStudentsWithClasses(activeAcademicYear.id)
    }

    suspend fun transferStudents(request: BulkTransferStudentsRequest): Int {
        validateUUID(request.fromClassId, "From Class ID")
        validateUUID(request.toClassId, "To Class ID")

        // Use active academic year if not provided
        val academicYearId = request.academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        if (request.studentIds.isEmpty()) {
            throw ApiException("Student IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        if (request.fromClassId == request.toClassId) {
            throw ApiException("Source and destination classes cannot be the same", HttpStatusCode.BadRequest)
        }

        // Validate entities exist
        classService.getClassById(request.fromClassId)
        classService.getClassById(request.toClassId)
        academicYearService.getAcademicYearById(academicYearId)

        // Validate students exist
        request.studentIds.forEach { studentId ->
            validateUUID(studentId, "Student ID")
            userService.getUserById(studentId)
        }

        return studentAssignmentRepository.transferStudents(
            request.studentIds,
            request.fromClassId,
            request.toClassId,
            academicYearId
        )
    }

    suspend fun removeAllStudentsFromClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return studentAssignmentRepository.deleteByClassId(classId)
    }

    suspend fun removeAllStudentsFromClassForActiveYear(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.deleteByClassAndAcademicYear(classId, activeAcademicYear.id)
    }

    suspend fun removeStudentFromAllClasses(studentId: String): Int {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        userService.getUserById(studentId)
        return studentAssignmentRepository.deleteByStudentId(studentId)
    }

    suspend fun removeStudentFromAllClassesForActiveYear(studentId: String): Int {
        validateUUID(studentId, "Student ID")
        // Validate student exists
        userService.getUserById(studentId)

        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.deleteByStudentAndAcademicYear(studentId, activeAcademicYear.id)
    }

    suspend fun getStudentCountByClass(academicYearId: String? = null): List<Pair<String, Int>> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(finalAcademicYearId)
        return studentAssignmentRepository.getStudentCountByClass(finalAcademicYearId)
    }

    suspend fun getStudentCountByClassForActiveYear(): List<Pair<String, Int>> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.getStudentCountByClass(activeAcademicYear.id)
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

    suspend fun getAcademicYearSummary(academicYearId: String? = null): AcademicYearSummaryDto {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")
        val academicYear = academicYearService.getAcademicYearById(finalAcademicYearId)

        val assignments = studentAssignmentRepository.findByAcademicYear(finalAcademicYearId)
        val classCounts = studentAssignmentRepository.getStudentCountByClass(finalAcademicYearId)

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
            academicYearId = finalAcademicYearId,
            academicYearName = academicYear.year,
            totalStudents = uniqueStudents,
            totalClasses = uniqueClasses,
            averageStudentsPerClass = if (uniqueClasses > 0) uniqueStudents.toDouble() / uniqueClasses else 0.0,
            classesWithStudents = classCapacities
        )
    }

    suspend fun getAcademicYearSummaryForActiveYear(): AcademicYearSummaryDto {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return getAcademicYearSummary(activeAcademicYear.id)
    }

    suspend fun validateStudentAssignment(studentId: String, classId: String, academicYearId: String? = null): Boolean {
        return try {
            // Use active academic year if not provided
            val finalAcademicYearId = academicYearId ?: run {
                val activeAcademicYear = academicYearService.getActiveAcademicYear()
                activeAcademicYear.id
            }

            validateStudentAssignmentRequest(studentId, classId, finalAcademicYearId)
            userService.getUserById(studentId)
            classService.getClassById(classId)
            academicYearService.getAcademicYearById(finalAcademicYearId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getStudentsNotInClass(classId: String, academicYearId: String? = null): List<StudentDto> {
        validateUUID(classId, "Class ID")

        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        // Validate that referenced entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(finalAcademicYearId)

        return studentAssignmentRepository.findStudentsNotInClass(classId, finalAcademicYearId)
    }

    suspend fun getStudentsNotInClassForActiveYear(classId: String): List<StudentDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return getStudentsNotInClass(classId, activeAcademicYear.id)
    }

    suspend fun getUnassignedStudents(academicYearId: String? = null): List<StudentDto> {
        // Use active academic year if not provided
        val finalAcademicYearId = academicYearId ?: run {
            val activeAcademicYear = academicYearService.getActiveAcademicYear()
            activeAcademicYear.id
        }

        validateUUID(finalAcademicYearId, "Academic Year ID")

        // Validate that academic year exists
        academicYearService.getAcademicYearById(finalAcademicYearId)

        return studentAssignmentRepository.findUnassignedStudents(finalAcademicYearId)
    }

    suspend fun getUnassignedStudentsForActiveYear(): List<StudentDto> {
        val activeAcademicYear = academicYearService.getActiveAcademicYear()
        return studentAssignmentRepository.findUnassignedStudents(activeAcademicYear.id)
    }

    suspend fun getAssignmentHistory(studentId: String): List<StudentAssignmentDto> {
        validateUUID(studentId, "Student ID")

        // Validate that student exists
        userService.getUserById(studentId)

        return studentAssignmentRepository.getAssignmentHistory(studentId)
    }
}