package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateStudentTransportAssignmentRequest
import com.example.models.dto.CreateStudentTransportAssignmentRequest
import com.example.models.dto.UpdateStudentTransportAssignmentRequest
import com.example.models.responses.ApiResponse
import com.example.services.StudentTransportAssignmentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentTransportAssignmentRoutes(studentTransportAssignmentService: StudentTransportAssignmentService) {
    route("/api/v1/student-transport-assignments") {

        // Get all student transport assignments
        get {
            val assignments = studentTransportAssignmentService.getAllStudentTransportAssignments()
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get active student transport assignments
        get("/active") {
            val assignments = studentTransportAssignmentService.getActiveStudentTransportAssignments()
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get active assignments by academic year
        get("/active/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic year ID is required", HttpStatusCode.BadRequest)

            val assignments = studentTransportAssignmentService.getActiveStudentTransportAssignmentsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get assignments by student ID
        get("/student/{studentId}") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val assignments = studentTransportAssignmentService.getStudentTransportAssignmentsByStudentId(studentId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get assignments by academic year ID
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic year ID is required", HttpStatusCode.BadRequest)

            val assignments = studentTransportAssignmentService.getStudentTransportAssignmentsByAcademicYearId(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get assignments by route ID
        get("/route/{routeId}") {
            val routeId = call.parameters["routeId"]
                ?: throw ApiException("Route ID is required", HttpStatusCode.BadRequest)

            val assignments = studentTransportAssignmentService.getStudentTransportAssignmentsByRouteId(routeId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get assignments by stop ID
        get("/stop/{stopId}") {
            val stopId = call.parameters["stopId"]
                ?: throw ApiException("Stop ID is required", HttpStatusCode.BadRequest)

            val assignments = studentTransportAssignmentService.getStudentTransportAssignmentsByStopId(stopId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get assignment by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            val assignment = studentTransportAssignmentService.getStudentTransportAssignmentById(id)
            call.respond(ApiResponse(
                success = true,
                data = assignment
            ))
        }

        // Create student transport assignment
        post {
            val request = call.receive<CreateStudentTransportAssignmentRequest>()
            val assignment = studentTransportAssignmentService.createStudentTransportAssignment(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = assignment,
                message = "Student transport assignment created successfully"
            ))
        }

        // Bulk create student transport assignments
        post("/bulk") {
            val request = call.receive<BulkCreateStudentTransportAssignmentRequest>()
            val assignments = studentTransportAssignmentService.bulkCreateStudentTransportAssignments(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = assignments,
                message = "Student transport assignments created successfully"
            ))
        }

        // Update student transport assignment
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateStudentTransportAssignmentRequest>()
            val assignment = studentTransportAssignmentService.updateStudentTransportAssignment(id, request)
            call.respond(ApiResponse(
                success = true,
                data = assignment,
                message = "Student transport assignment updated successfully"
            ))
        }

        // Toggle assignment status
        patch("/{id}/toggle-status") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            val assignment = studentTransportAssignmentService.toggleStudentTransportAssignmentStatus(id)
            call.respond(ApiResponse(
                success = true,
                data = assignment,
                message = "Student transport assignment status updated successfully"
            ))
        }

        // Delete student transport assignment
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            studentTransportAssignmentService.deleteStudentTransportAssignment(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Student transport assignment deleted successfully"
            ))
        }
    }
}
