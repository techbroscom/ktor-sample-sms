package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.FeePaymentService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.feePaymentRoutes(feePaymentService: FeePaymentService) {
    route("/api/v1/fee-payments") {

        // Get all fee payments
        get {
            val payments = feePaymentService.getAllFeePayments()
            call.respond(ApiResponse(
                success = true,
                data = payments,
                message = "Fee payments retrieved successfully"
            ))
        }

        // Get fee payment by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Payment ID is required", HttpStatusCode.BadRequest)

            val payment = feePaymentService.getFeePaymentById(id)
            call.respond(ApiResponse(
                success = true,
                data = payment,
                message = "Fee payment retrieved successfully"
            ))
        }

        // Create new fee payment
        post {
            val request = call.receive<CreateFeePaymentRequest>()
            val payment = feePaymentService.createFeePayment(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = payment,
                message = "Fee payment created successfully"
            ))
        }

        // Update fee payment
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Payment ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateFeePaymentRequest>()
            val payment = feePaymentService.updateFeePayment(id, request)
            call.respond(ApiResponse(
                success = true,
                data = payment,
                message = "Fee payment updated successfully"
            ))
        }

        // Delete fee payment
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Payment ID is required", HttpStatusCode.BadRequest)

            feePaymentService.deleteFeePayment(id)
            call.respond(
                ApiResponse<Unit>(
                success = true,
                message = "Fee payment deleted successfully"
            )
            )
        }

        // Get payments by student fee ID
        get("/student-fee/{studentFeeId}") {
            val studentFeeId = call.parameters["studentFeeId"]
                ?: throw ApiException("Student fee ID is required", HttpStatusCode.BadRequest)

            val payments = feePaymentService.getFeePaymentsByStudentFeeId(studentFeeId)
            call.respond(ApiResponse(
                success = true,
                data = payments,
                message = "Payments retrieved successfully"
            ))
        }

        // Get payments by student ID
        get("/student/{studentId}") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val payments = feePaymentService.getFeePaymentsByStudentId(studentId)
            call.respond(ApiResponse(
                success = true,
                data = payments,
                message = "Student payments retrieved successfully"
            ))
        }

        // Get payments by payment mode
        get("/payment-mode/{paymentMode}") {
            val paymentMode = call.parameters["paymentMode"]
                ?: throw ApiException("Payment mode is required", HttpStatusCode.BadRequest)

            val payments = feePaymentService.getFeePaymentsByPaymentMode(paymentMode)
            call.respond(ApiResponse(
                success = true,
                data = payments,
                message = "Payments by payment mode retrieved successfully"
            ))
        }

        // Get payments by date range
        get("/date-range") {
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)

            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)

            val payments = feePaymentService.getFeePaymentsByDateRange(startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = payments,
                message = "Payments by date range retrieved successfully"
            ))
        }

        // Get payments by month
        get("/month/{month}") {
            val month = call.parameters["month"]
                ?: throw ApiException("Month is required", HttpStatusCode.BadRequest)

            val payments = feePaymentService.getFeePaymentsByMonth(month)
            call.respond(ApiResponse(
                success = true,
                data = payments,
                message = "Payments by month retrieved successfully"
            ))
        }

        // Get payments by class and date range
        get("/class/{classId}/date-range") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)

            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)

            val payments = feePaymentService.getFeePaymentsByClassAndDateRange(classId, startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = payments,
                message = "Class payments by date range retrieved successfully"
            ))
        }

        // Get payment by receipt number
        get("/receipt/{receiptNumber}") {
            val receiptNumber = call.parameters["receiptNumber"]
                ?: throw ApiException("Receipt number is required", HttpStatusCode.BadRequest)

            val payment = feePaymentService.getFeePaymentByReceiptNumber(receiptNumber)
            if (payment != null) {
                call.respond(ApiResponse(
                    success = true,
                    data = payment,
                    message = "Payment retrieved successfully"
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(
                    success = false,
                    message = "Payment not found"
                )
                )
            }
        }

        // Get payment summary for a student
        get("/student/{studentId}/summary") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val summary = feePaymentService.getPaymentSummary(studentId)
            if (summary != null) {
                call.respond(ApiResponse(
                    success = true,
                    data = summary,
                    message = "Payment summary retrieved successfully"
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(
                    success = false,
                    message = "No payment records found for student"
                ))
            }
        }

        // Get daily payment report
        get("/reports/daily/{date}") {
            val date = call.parameters["date"]
                ?: throw ApiException("Date is required", HttpStatusCode.BadRequest)

            val report = feePaymentService.getDailyPaymentReport(date)
            call.respond(ApiResponse(
                success = true,
                data = report,
                message = "Daily payment report retrieved successfully"
            ))
        }

        // Get monthly payment report
        get("/reports/monthly/{month}") {
            val month = call.parameters["month"]
                ?: throw ApiException("Month is required", HttpStatusCode.BadRequest)

            val report = feePaymentService.getMonthlyPaymentReport(month)
            call.respond(ApiResponse(
                success = true,
                data = report,
                message = "Monthly payment report retrieved successfully"
            ))
        }

        // Get class payment summary
        get("/class/{classId}/summary") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)

            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)

            val summary = feePaymentService.getClassPaymentSummary(classId, startDate, endDate)
            if (summary != null) {
                call.respond(ApiResponse(
                    success = true,
                    data = summary,
                    message = "Class payment summary retrieved successfully"
                ))
            } else {
                call.respond(HttpStatusCode.NotFound, ApiResponse<Unit>(
                    success = false,
                    message = "No payment records found for class"
                ))
            }
        }

        // Bulk delete payments by student fee ID
        delete("/student-fee/{studentFeeId}/bulk") {
            val studentFeeId = call.parameters["studentFeeId"]
                ?: throw ApiException("Student fee ID is required", HttpStatusCode.BadRequest)

            val deletedCount = feePaymentService.deletePaymentsByStudentFeeId(studentFeeId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "Payments deleted successfully"
            ))
        }

        // Bulk delete payments by student ID
        delete("/student/{studentId}/bulk") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val deletedCount = feePaymentService.deletePaymentsByStudentId(studentId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "Payments deleted successfully"
            ))
        }
    }
}