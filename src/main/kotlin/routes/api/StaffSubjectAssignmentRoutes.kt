package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.StaffSubjectAssignmentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.staffSubjectAssignmentRoutes(staffSubjectAssignmentService: StaffSubjectAssignmentService) {
    route("/api/v1/staff-subject-assignments") {

        // Get all staff subject assignments
        get {
            val assignments = staffSubjectAssignmentService.getAllStaffSubjectAssignments()
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get all staff subject assignments for active academic year
        get("/active-year") {
            val assignments = staffSubjectAssignmentService.getStaffSubjectAssignmentsForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff subject assignment by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            val assignment = staffSubjectAssignmentService.getStaffSubjectAssignmentById(id)
            call.respond(ApiResponse(
                success = true,
                data = assignment
            ))
        }

        // Get assignments by academic year
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getStaffSubjectAssignmentsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get subjects by staff ID
        get("/staff/{staffId}/subjects") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getSubjectsByStaff(staffId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get subjects by staff ID for active academic year
        get("/staff/{staffId}/subjects/active-year") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getSubjectsByStaffForActiveYear(staffId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by class ID
        get("/class/{classId}/staff") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getStaffByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by class ID for active academic year
        get("/class/{classId}/staff/active-year") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getStaffByClassForActiveYear(classId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by class subject ID
        get("/class-subject/{classSubjectId}/staff") {
            val classSubjectId = call.parameters["classSubjectId"]
                ?: throw ApiException("Class Subject ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getStaffByClassSubject(classSubjectId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by class subject ID for active academic year
        get("/class-subject/{classSubjectId}/staff/active-year") {
            val classSubjectId = call.parameters["classSubjectId"]
                ?: throw ApiException("Class Subject ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getStaffByClassSubjectForActiveYear(classSubjectId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get subjects by staff and academic year
        get("/staff/{staffId}/academic-year/{academicYearId}/subjects") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getSubjectsByStaffAndAcademicYear(staffId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by class and academic year
        get("/class/{classId}/academic-year/{academicYearId}/staff") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val assignments = staffSubjectAssignmentService.getStaffByClassAndAcademicYear(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff with their subjects (grouped view)
        get("/academic-year/{academicYearId}/staff-with-subjects") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val staffWithSubjects = staffSubjectAssignmentService.getStaffWithSubjects(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = staffWithSubjects
            ))
        }

        // Get staff with their subjects for active academic year (grouped view)
        get("/active-year/staff-with-subjects") {
            val staffWithSubjects = staffSubjectAssignmentService.getStaffWithSubjectsForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = staffWithSubjects
            ))
        }

        // Get classes with their subjects and staff (grouped view)
        get("/academic-year/{academicYearId}/classes-with-subject-staff") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classesWithSubjectStaff = staffSubjectAssignmentService.getClassesWithSubjectStaff(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classesWithSubjectStaff
            ))
        }

        // Get classes with their subjects and staff for active academic year (grouped view)
        get("/active-year/classes-with-subject-staff") {
            val classesWithSubjectStaff = staffSubjectAssignmentService.getClassesWithSubjectStaffForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = classesWithSubjectStaff
            ))
        }

        // Create a new staff subject assignment
        post {
            val request = call.receive<CreateStaffSubjectAssignmentRequest>()
            val assignment = staffSubjectAssignmentService.createStaffSubjectAssignment(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = assignment,
                message = "Staff subject assignment created successfully"
            ))
        }

        // Bulk create staff subject assignments
        post("/bulk") {
            val request = call.receive<BulkCreateStaffSubjectAssignmentRequest>()
            val assignments = staffSubjectAssignmentService.bulkCreateStaffSubjectAssignments(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = assignments,
                message = "Staff subject assignments created successfully"
            ))
        }

        // Update staff subject assignment
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateStaffSubjectAssignmentRequest>()
            val assignment = staffSubjectAssignmentService.updateStaffSubjectAssignment(id, request)
            call.respond(ApiResponse(
                success = true,
                data = assignment,
                message = "Staff subject assignment updated successfully"
            ))
        }

        // Delete staff subject assignment
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            staffSubjectAssignmentService.deleteStaffSubjectAssignment(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Staff subject assignment deleted successfully"
            ))
        }

        // Delete assignments by staff ID
        delete("/staff/{staffId}/assignments") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffSubjectAssignmentService.deleteStaffSubjectAssignmentsByStaff(staffId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All subject assignments removed from staff member"
            ))
        }

        // Delete assignments by staff ID for active academic year
        delete("/staff/{staffId}/assignments/active-year") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffSubjectAssignmentService.deleteStaffSubjectAssignmentsByStaffForActiveYear(staffId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All subject assignments removed from staff member for active academic year"
            ))
        }

        // Delete assignments by class ID
        delete("/class/{classId}/assignments") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffSubjectAssignmentService.deleteStaffSubjectAssignmentsByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All subject assignments removed from class"
            ))
        }

        // Delete assignments by class ID for active academic year
        delete("/class/{classId}/assignments/active-year") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffSubjectAssignmentService.deleteStaffSubjectAssignmentsByClassForActiveYear(classId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All subject assignments removed from class for active academic year"
            ))
        }

        // Delete assignments by class subject ID
        delete("/class-subject/{classSubjectId}/assignments") {
            val classSubjectId = call.parameters["classSubjectId"]
                ?: throw ApiException("Class Subject ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffSubjectAssignmentService.deleteStaffSubjectAssignmentsByClassSubject(classSubjectId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All assignments removed from class subject"
            ))
        }

        // Delete assignments by class subject ID for active academic year
        delete("/class-subject/{classSubjectId}/assignments/active-year") {
            val classSubjectId = call.parameters["classSubjectId"]
                ?: throw ApiException("Class Subject ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffSubjectAssignmentService.deleteStaffSubjectAssignmentsByClassSubjectForActiveYear(classSubjectId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All assignments removed from class subject for active academic year"
            ))
        }

        // Delete assignments by academic year ID
        delete("/academic-year/{academicYearId}/assignments") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffSubjectAssignmentService.deleteStaffSubjectAssignmentsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All assignments removed from academic year"
            ))
        }
    }
}