package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.StudentFeeService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.studentFeeRoutes(studentFeeService: StudentFeeService) {
    route("/api/v1/student-fees") {

        // Get all student fees
        get {
            val fees = studentFeeService.getAllStudentFees()
            call.respond(ApiResponse(
                success = true,
                data = fees,
                message = "Student fees retrieved successfully"
            ))
        }

        // Get student fee by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Student fee ID is required", HttpStatusCode.BadRequest)

            val fee = studentFeeService.getStudentFeeById(id)
            call.respond(ApiResponse(
                success = true,
                data = fee,
                message = "Student fee retrieved successfully"
            ))
        }

        // Create student fee
        post {
            val request = call.receive<CreateStudentFeeRequest>()
            val fee = studentFeeService.createStudentFee(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = fee,
                message = "Student fee created successfully"
            ))
        }

        // Bulk create student fees
        post("/bulk") {
            val request = call.receive<BulkCreateStudentFeeRequest>()
            val fees = studentFeeService.bulkCreateStudentFees(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = fees,
                message = "Student fees created successfully"
            ))
        }

        // Update student fee
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Student fee ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateStudentFeeRequest>()
            val fee = studentFeeService.updateStudentFee(id, request)
            call.respond(ApiResponse(
                success = true,
                data = fee,
                message = "Student fee updated successfully"
            ))
        }

        // Delete student fee
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Student fee ID is required", HttpStatusCode.BadRequest)

            studentFeeService.deleteStudentFee(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Student fee deleted successfully"
            ))
        }

        // Pay fee
        post("/{id}/pay") {
            val id = call.parameters["id"]
                ?: throw ApiException("Student fee ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<PayFeeRequest>()
            val fee = studentFeeService.payFee(id, request)
            call.respond(ApiResponse(
                success = true,
                data = fee,
                message = "Fee payment processed successfully"
            ))
        }

        // Get fees by student ID
        get("/student/{studentId}") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val fees = studentFeeService.getStudentFeesByStudentId(studentId)
            call.respond(ApiResponse(
                success = true,
                data = fees,
                message = "Student fees retrieved successfully"
            ))
        }

        // Get student fees summary
        get("/student/{studentId}/summary") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val summary = studentFeeService.getStudentFeesSummary(studentId)
            call.respond(ApiResponse(
                success = true,
                data = summary,
                message = "Student fees summary retrieved successfully"
            ))
        }

        // Get fees by fee structure ID
        get("/fee-structure/{feeStructureId}") {
            val feeStructureId = call.parameters["feeStructureId"]
                ?: throw ApiException("Fee structure ID is required", HttpStatusCode.BadRequest)

            val fees = studentFeeService.getStudentFeesByFeeStructureId(feeStructureId)
            call.respond(ApiResponse(
                success = true,
                data = fees,
                message = "Student fees retrieved successfully"
            ))
        }

        // Get fees by month
        get("/month/{month}") {
            val month = call.parameters["month"]
                ?: throw ApiException("Month is required", HttpStatusCode.BadRequest)

            val fees = studentFeeService.getStudentFeesByMonth(month)
            call.respond(ApiResponse(
                success = true,
                data = fees,
                message = "Student fees retrieved successfully"
            ))
        }

        // Get monthly fee report
        get("/reports/monthly/{month}") {
            val month = call.parameters["month"]
                ?: throw ApiException("Month is required", HttpStatusCode.BadRequest)

            val report = studentFeeService.getMonthlyFeeReport(month)
            call.respond(ApiResponse(
                success = true,
                data = report,
                message = "Monthly fee report retrieved successfully"
            ))
        }

        // Get fees by status
        get("/status/{status}") {
            val status = call.parameters["status"]
                ?: throw ApiException("Status is required", HttpStatusCode.BadRequest)

            val fees = studentFeeService.getStudentFeesByStatus(status)
            call.respond(ApiResponse(
                success = true,
                data = fees,
                message = "Student fees retrieved successfully"
            ))
        }

        // Get overdue fees
        get("/overdue") {
            val fees = studentFeeService.getOverdueFees()
            call.respond(ApiResponse(
                success = true,
                data = fees,
                message = "Overdue fees retrieved successfully"
            ))
        }

        // Get fees by class and month
        get("/class/{classId}/month/{month}") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val month = call.parameters["month"]
                ?: throw ApiException("Month is required", HttpStatusCode.BadRequest)

            val fees = studentFeeService.getStudentFeesByClassAndMonth(classId, month)
            call.respond(ApiResponse(
                success = true,
                data = fees,
                message = "Student fees retrieved successfully"
            ))
        }

        // Get class fees summary
        get("/class/{classId}/month/{month}/summary") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val month = call.parameters["month"]
                ?: throw ApiException("Month is required", HttpStatusCode.BadRequest)

            val summary = studentFeeService.getClassFeesSummary(classId, month)
            call.respond(ApiResponse(
                success = true,
                data = summary,
                message = "Class fees summary retrieved successfully"
            ))
        }

        // Remove all fees for a student
        delete("/student/{studentId}") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val deletedCount = studentFeeService.removeStudentFees(studentId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "Student fees removed successfully"
            ))
        }

        // Remove all fees for a fee structure
        delete("/fee-structure/{feeStructureId}") {
            val feeStructureId = call.parameters["feeStructureId"]
                ?: throw ApiException("Fee structure ID is required", HttpStatusCode.BadRequest)

            val deletedCount = studentFeeService.removeFeesByStructure(feeStructureId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "Fees removed successfully"
            ))
        }
    }
}
