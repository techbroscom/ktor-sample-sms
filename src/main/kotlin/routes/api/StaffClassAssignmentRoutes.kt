package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateStaffClassAssignmentRequest
import com.example.models.dto.CreateStaffClassAssignmentRequest
import com.example.models.dto.UpdateStaffClassAssignmentRequest
import com.example.models.responses.ApiResponse
import com.example.services.StaffClassAssignmentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.staffClassAssignmentRoutes(staffClassAssignmentService: StaffClassAssignmentService) {
    route("/api/v1/staff-class-assignments") {

        // Get all staff-class assignments
        get {
            val assignments = staffClassAssignmentService.getAllStaffClassAssignments()
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff-class assignments for active academic year
        get("/active-year") {
            val assignments = staffClassAssignmentService.getStaffClassAssignmentsForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff-class assignments by academic year
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getStaffClassAssignmentsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get classes by staff (all years)
        get("/staff/{staffId}/classes") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getClassesByStaff(staffId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get classes by staff for active academic year
        get("/staff/{staffId}/classes/active-year") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getClassesByStaffForActiveYear(staffId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by class (all years)
        get("/class/{classId}/staff") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getStaffByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by class for active academic year
        get("/class/{classId}/staff/active-year") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getStaffByClassForActiveYear(classId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get classes by staff and academic year
        get("/staff/{staffId}/academic-year/{academicYearId}/classes") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getClassesByStaffAndAcademicYear(staffId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by role (all years)
        get("/role/{role}") {
            val role = call.parameters["role"]
                ?: throw ApiException("Role is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getStaffByRole(role)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff by role for active academic year
        get("/role/{role}/active-year") {
            val role = call.parameters["role"]
                ?: throw ApiException("Role is required", HttpStatusCode.BadRequest)

            val assignments = staffClassAssignmentService.getStaffByRoleForActiveYear(role)
            call.respond(ApiResponse(
                success = true,
                data = assignments
            ))
        }

        // Get staff with their classes for an academic year
        get("/academic-year/{academicYearId}/staff-with-classes") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val staffWithClasses = staffClassAssignmentService.getStaffWithClasses(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = staffWithClasses
            ))
        }

        // Get staff with their classes for active academic year
        get("/active-year/staff-with-classes") {
            val staffWithClasses = staffClassAssignmentService.getStaffWithClassesForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = staffWithClasses
            ))
        }

        // Get classes with their staff for an academic year
        get("/academic-year/{academicYearId}/classes-with-staff") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classesWithStaff = staffClassAssignmentService.getClassesWithStaff(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classesWithStaff
            ))
        }

        // Get classes with their staff for active academic year
        get("/active-year/classes-with-staff") {
            val classesWithStaff = staffClassAssignmentService.getClassesWithStaffForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = classesWithStaff
            ))
        }

        // Get specific staff-class assignment by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            val assignment = staffClassAssignmentService.getStaffClassAssignmentById(id)
            call.respond(ApiResponse(
                success = true,
                data = assignment
            ))
        }

        // Create new staff-class assignment
        post {
            val request = call.receive<CreateStaffClassAssignmentRequest>()
            val assignment = staffClassAssignmentService.createStaffClassAssignment(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = assignment,
                message = "Staff-class assignment created successfully"
            ))
        }

        // Bulk create staff-class assignments
        post("/bulk") {
            val request = call.receive<BulkCreateStaffClassAssignmentRequest>()
            val assignments = staffClassAssignmentService.bulkCreateStaffClassAssignments(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = assignments,
                message = "Staff-class assignments created successfully"
            ))
        }

        // Update staff-class assignment
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateStaffClassAssignmentRequest>()
            val assignment = staffClassAssignmentService.updateStaffClassAssignment(id, request)
            call.respond(ApiResponse(
                success = true,
                data = assignment,
                message = "Staff-class assignment updated successfully"
            ))
        }

        // Delete staff-class assignment
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assignment ID is required", HttpStatusCode.BadRequest)

            staffClassAssignmentService.deleteStaffClassAssignment(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Staff-class assignment deleted successfully"
            ))
        }

        // Remove all classes from a staff member (all years)
        delete("/staff/{staffId}/classes") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffClassAssignmentService.removeAllClassesFromStaff(staffId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All class assignments removed from staff member"
            ))
        }

        // Remove all classes from a staff member for active academic year only
        delete("/staff/{staffId}/classes/active-year") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffClassAssignmentService.removeAllClassesFromStaffForActiveYear(staffId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All class assignments removed from staff member for active academic year"
            ))
        }

        // Remove all staff from a class (all years)
        delete("/class/{classId}/staff") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffClassAssignmentService.removeAllStaffFromClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All staff assignments removed from class"
            ))
        }

        // Remove all staff from a class for active academic year only
        delete("/class/{classId}/staff/active-year") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = staffClassAssignmentService.removeAllStaffFromClassForActiveYear(classId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All staff assignments removed from class for active academic year"
            ))
        }
    }
}