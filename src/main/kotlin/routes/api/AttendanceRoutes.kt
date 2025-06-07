package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateAttendanceRequest
import com.example.models.dto.CreateAttendanceRequest
import com.example.models.dto.UpdateAttendanceRequest
import com.example.models.responses.ApiResponse
import com.example.services.AttendanceService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.attendanceRoutes(attendanceService: AttendanceService) {
    route("/api/v1/attendance") {

        // Get all attendance records
        get {
            val attendance = attendanceService.getAllAttendance()
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Get attendance by student
        get("/student/{studentId}") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val attendance = attendanceService.getAttendanceByStudent(studentId)
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Get attendance by class
        get("/class/{classId}") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val attendance = attendanceService.getAttendanceByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Get attendance by date
        get("/date/{date}") {
            val date = call.parameters["date"]
                ?: throw ApiException("Date is required", HttpStatusCode.BadRequest)

            val attendance = attendanceService.getAttendanceByDate(date)
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Get attendance by class and date
        get("/class/{classId}/date/{date}") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val date = call.parameters["date"]
                ?: throw ApiException("Date is required", HttpStatusCode.BadRequest)

            val attendance = attendanceService.getAttendanceByClassAndDate(classId, date)
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Get attendance by student and date range
        get("/student/{studentId}/range") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)

            val attendance = attendanceService.getAttendanceByStudentAndDateRange(studentId, startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Get attendance by class and date range
        get("/class/{classId}/range") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)

            val attendance = attendanceService.getAttendanceByClassAndDateRange(classId, startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Get attendance stats for student
        get("/student/{studentId}/stats") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)

            val stats = attendanceService.getAttendanceStats(studentId, startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = stats
            ))
        }

        // Get class attendance summary for a specific date
        get("/class/{classId}/date/{date}/summary") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val date = call.parameters["date"]
                ?: throw ApiException("Date is required", HttpStatusCode.BadRequest)

            val summary = attendanceService.getClassAttendanceForDate(classId, date)
            call.respond(ApiResponse(
                success = true,
                data = summary
            ))
        }

        // Get attendance record by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Attendance ID is required", HttpStatusCode.BadRequest)

            val attendance = attendanceService.getAttendanceById(id)
            call.respond(ApiResponse(
                success = true,
                data = attendance
            ))
        }

        // Create attendance record
        post {
            val request = call.receive<CreateAttendanceRequest>()
            val attendance = attendanceService.createAttendance(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                message = "Attendance record created successfully",
                data = attendance
            ))
        }

        // Bulk create attendance records
        post("/bulk") {
            val request = call.receive<BulkCreateAttendanceRequest>()
            val attendanceRecords = attendanceService.bulkCreateAttendance(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                message = "Attendance records created successfully",
                data = attendanceRecords
            ))
        }

        // Update attendance record
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Attendance ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateAttendanceRequest>()

            val attendance = attendanceService.updateAttendance(id, request)
            call.respond(ApiResponse(
                success = true,
                message = "Attendance record updated successfully",
                data = attendance
            ))
        }

        // Delete attendance record
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Attendance ID is required", HttpStatusCode.BadRequest)

            attendanceService.deleteAttendance(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Attendance record deleted successfully"
            ))
        }

        // Delete all attendance records for a student
        delete("/student/{studentId}") {
            val studentId = call.parameters["studentId"]
                ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)

            val deletedCount = attendanceService.removeAllAttendanceForStudent(studentId)
            call.respond(ApiResponse(
                success = true,
                message = "All attendance records for student deleted successfully",
                data = mapOf("deletedCount" to deletedCount)
            ))
        }

        // Delete all attendance records for a class
        delete("/class/{classId}") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = attendanceService.removeAllAttendanceForClass(classId)
            call.respond(ApiResponse(
                success = true,
                message = "All attendance records for class deleted successfully",
                data = mapOf("deletedCount" to deletedCount)
            ))
        }

        // Delete all attendance records for a specific date
        delete("/date/{date}") {
            val date = call.parameters["date"]
                ?: throw ApiException("Date is required", HttpStatusCode.BadRequest)

            val deletedCount = attendanceService.removeAllAttendanceForDate(date)
            call.respond(ApiResponse(
                success = true,
                message = "All attendance records for date deleted successfully",
                data = mapOf("deletedCount" to deletedCount)
            ))
        }
    }
}