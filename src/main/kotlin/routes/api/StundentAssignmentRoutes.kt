package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.StudentAssignmentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentAssignmentRoutes(studentAssignmentService: StudentAssignmentService) {
    route("/api/v1/student-assignments") {

        // Get all student assignments
        get {
            val assignments = studentAssignmentService.getAllStudentAssignments()
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get student assignments by academic year
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val assignments = studentAssignmentService.getStudentAssignmentsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get students by class
        get("/class/{classId}/students") {
            val classId = call.getPathParameter("classId", "Class ID")
            val assignments = studentAssignmentService.getClassStudents(classId)
            call.respond(ApiResponse(success = true, data = assignments))
        }

        // Get classes by student
        get("/student/{studentId}/classes") {
            val studentId = call.getPathParameter("studentId", "Student ID")
            val assignments = studentAssignmentService.getStudentAssignments(studentId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get students by class and academic year
        get("/class/{classId}/academic-year/{academicYearId}/students") {
            val classId = call.getPathParameter("classId", "Class ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val assignments = studentAssignmentService.getClassStudentsByAcademicYear(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get student's current class for academic year
        get("/student/{studentId}/academic-year/{academicYearId}/current-class") {
            val studentId = call.getPathParameter("studentId", "Student ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val assignment = studentAssignmentService.getStudentCurrentClass(studentId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignment
            ))
        }

        // Get classes with their students for an academic year
        get("/academic-year/{academicYearId}/classes-with-students") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val classesWithStudents = studentAssignmentService.getClassesWithStudents(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classesWithStudents
            ))
        }

        // Get students with their classes for an academic year
        get("/academic-year/{academicYearId}/students-with-classes") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val studentsWithClasses = studentAssignmentService.getStudentsWithClasses(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = studentsWithClasses
            ))
        }

        // Get student count by class for an academic year
        get("/academic-year/{academicYearId}/student-count-by-class") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val counts = studentAssignmentService.getStudentCountByClass(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = counts.map { (className, count) ->
                    mapOf("className" to className, "studentCount" to count)
                }
            ))
        }

        // ================================
        // NEW EXTENDED ROUTES
        // ================================

        // Get student assignment summary (analytics for a specific student)
        get("/student/{studentId}/summary") {
            val studentId = call.getPathParameter("studentId", "Student ID")
            val summary = studentAssignmentService.getStudentAssignmentSummary(studentId)
            call.respond(ApiResponse(
                success = true,
                data = summary
            ))
        }

        // Get academic year summary (comprehensive analytics)
        get("/academic-year/{academicYearId}/summary") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val summary = studentAssignmentService.getAcademicYearSummary(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = summary
            ))
        }

        // Get students not assigned to a specific class (for bulk assignment)
        get("/class/{classId}/academic-year/{academicYearId}/available-students") {
            val classId = call.getPathParameter("classId", "Class ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val availableStudents = studentAssignmentService.getStudentsNotInClass(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = availableStudents
            ))
        }

        // Get unassigned students for an academic year
        get("/academic-year/{academicYearId}/unassigned-students") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val unassignedStudents = studentAssignmentService.getUnassignedStudents(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = unassignedStudents
            ))
        }

        // Get complete assignment history for a student
        get("/student/{studentId}/history") {
            val studentId = call.getPathParameter("studentId", "Student ID")
            val history = studentAssignmentService.getAssignmentHistory(studentId)
            call.respond(ApiResponse(
                success = true,
                data = history
            ))
        }

        // Validate student assignment (utility endpoint)
        get("/validate") {
            val studentId = call.request.queryParameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)
            val classId = call.request.queryParameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.request.queryParameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val isValid = studentAssignmentService.validateStudentAssignment(studentId, classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf(
                    "isValid" to isValid,
                    "studentId" to studentId,
                    "classId" to classId,
                    "academicYearId" to academicYearId
                )
            ))
        }

        // Get class roster with detailed student information
        get("/class/{classId}/academic-year/{academicYearId}/roster") {
            val classId = call.getPathParameter("classId", "Class ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val assignments = studentAssignmentService.getClassStudentsByAcademicYear(classId, academicYearId)

            // Enhanced roster with additional metadata
            val roster = mapOf(
                "classId" to classId,
                "academicYearId" to academicYearId,
                "totalStudents" to assignments.size,
                "students" to assignments,
                "generatedAt" to System.currentTimeMillis()
            )

            call.respond(ApiResponse(
                success = true,
                data = roster
            ))
        }

        // Bulk operations endpoint for complex assignments
        post("/bulk-operations") {
            val operation = call.request.queryParameters["operation"]
                ?: throw ApiException("Operation type is required", HttpStatusCode.BadRequest)

            when (operation.lowercase()) {
                "assign-all-to-class" -> {
                    val request = call.receive<BulkCreateStudentAssignmentRequest>()
                    val assignments = studentAssignmentService.bulkCreateStudentAssignments(request)
                    call.respond(HttpStatusCode.Created, ApiResponse(
                        success = true,
                        message = "${assignments.size} students assigned to class successfully",
                        data = assignments
                    ))
                }
                "transfer-between-classes" -> {
                    val request = call.receive<BulkTransferStudentsRequest>()
                    val transferredCount = studentAssignmentService.transferStudents(request)
                    call.respond(ApiResponse(
                        success = true,
                        message = "$transferredCount students transferred successfully",
                        data = mapOf("transferredCount" to transferredCount)
                    ))
                }
                else -> {
                    throw ApiException("Unsupported operation: $operation", HttpStatusCode.BadRequest)
                }
            }
        }

        // ================================
        // EXISTING ROUTES (kept as-is)
        // ================================

        // Create single student assignment
        post {
            val request = call.receive<CreateStudentAssignmentRequest>()
            val assignment = studentAssignmentService.createStudentAssignment(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                message = "Student assignment created successfully",
                data = assignment
            ))
        }

        // Bulk create student assignments
        post("/bulk") {
            val request = call.receive<BulkCreateStudentAssignmentRequest>()
            val assignments = studentAssignmentService.bulkCreateStudentAssignments(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                message = "${assignments.size} student assignments created successfully",
                data = assignments
            ))
        }

        // Transfer students between classes
        post("/transfer") {
            val request = call.receive<BulkTransferStudentsRequest>()
            val transferredCount = studentAssignmentService.transferStudents(request)
            call.respond(ApiResponse(
                success = true,
                message = "$transferredCount students transferred successfully",
                data = mapOf("transferredCount" to transferredCount)
            ))
        }

        // Get specific student assignment
        get("/{id}") {
            val id = call.getPathParameter("id", "Student Assignment ID")
            val assignment = studentAssignmentService.getStudentAssignmentById(id)
            call.respond(ApiResponse(
                success = true,
                data = assignment
            ))
        }

        // Update student assignment
        put("/{id}") {
            val id = call.getPathParameter("id", "Student Assignment ID")
            val request = call.receive<UpdateStudentAssignmentRequest>()
            val assignment = studentAssignmentService.updateStudentAssignment(id, request)
            call.respond(ApiResponse(
                success = true,
                message = "Student assignment updated successfully",
                data = assignment
            ))
        }

        // Delete student assignment
        delete("/{id}") {
            val id = call.getPathParameter("id", "Student Assignment ID")
            studentAssignmentService.deleteStudentAssignment(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Student assignment deleted successfully"
            ))
        }

        // Remove all students from a class
        delete("/class/{classId}/all-students") {
            val classId = call.getPathParameter("classId", "Class ID")
            val removedCount = studentAssignmentService.removeAllStudentsFromClass(classId)
            call.respond(ApiResponse(
                success = true,
                message = "$removedCount students removed from class",
                data = mapOf("removedCount" to removedCount)
            ))
        }

        // Remove student from all classes
        delete("/student/{studentId}/all-classes") {
            val studentId = call.getPathParameter("studentId", "Student ID")
            val removedCount = studentAssignmentService.removeStudentFromAllClasses(studentId)
            call.respond(ApiResponse(
                success = true,
                message = "Student removed from $removedCount classes",
                data = mapOf("removedCount" to removedCount)
            ))
        }
    }
}

// Extension function for cleaner parameter extraction
private fun ApplicationCall.getPathParameter(name: String, description: String): String {
    return parameters[name] ?: throw ApiException("$description is required", HttpStatusCode.BadRequest)
}