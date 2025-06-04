package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateExamScheduleRequest
import com.example.models.dto.CreateExamScheduleRequest
import com.example.models.dto.UpdateExamScheduleRequest
import com.example.models.responses.ApiResponse
import com.example.services.ExamScheduleService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.examScheduleRoutes(examScheduleService: ExamScheduleService) {
    route("/api/v1/exam-schedules") {
        // Get all exam schedules
        get {
            val examSchedules = examScheduleService.getAllExamSchedules()
            call.respond(ApiResponse(
                success = true,
                data = examSchedules
            ))
        }

        // Get specific exam schedule by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: throw ApiException("Schedule ID is required", HttpStatusCode.BadRequest)
            val examSchedule = examScheduleService.getExamScheduleById(id)
            call.respond(ApiResponse(
                success = true,
                data = examSchedule
            ))
        }

        // Create new exam schedule
        post {
            val request = call.receive<CreateExamScheduleRequest>()
            val examSchedule = examScheduleService.createExamSchedule(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = examSchedule,
                message = "Exam schedule created successfully"
            ))
        }

        // Bulk create exam schedules
        post("/bulk") {
            val request = call.receive<BulkCreateExamScheduleRequest>()
            val examSchedules = examScheduleService.bulkCreateExamSchedules(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = examSchedules,
                message = "Exam schedules created successfully"
            ))
        }

        // Update exam schedule
        put("/{id}") {
            val id = call.parameters["id"] ?: throw ApiException("Schedule ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateExamScheduleRequest>()
            val examSchedule = examScheduleService.updateExamSchedule(id, request)
            call.respond(ApiResponse(
                success = true,
                data = examSchedule,
                message = "Exam schedule updated successfully"
            ))
        }

        // Delete exam schedule
        delete("/{id}") {
            val id = call.parameters["id"] ?: throw ApiException("Schedule ID is required", HttpStatusCode.BadRequest)
            examScheduleService.deleteExamSchedule(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Exam schedule deleted successfully"
            ))
        }

        // Get exam schedules by exam
        get("/exam/{examId}") {
            val examId = call.parameters["examId"] ?: throw ApiException("Exam ID is required", HttpStatusCode.BadRequest)
            val examSchedules = examScheduleService.getSchedulesByExam(examId)
            call.respond(ApiResponse(
                success = true,
                data = examSchedules
            ))
        }

        // Get exam schedules by class
        get("/class/{classId}") {
            val classId = call.parameters["classId"] ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val examSchedules = examScheduleService.getSchedulesByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = examSchedules
            ))
        }

        // Get exam schedules by date range
        get("/date-range") {
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)

            val examSchedules = examScheduleService.getSchedulesByDateRange(startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = examSchedules
            ))
        }

        // Get exams with their schedules for an academic year
        get("/academic-year/{academicYearId}/exams-with-schedules") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val examsWithSchedules = examScheduleService.getExamsWithSchedules(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = examsWithSchedules
            ))
        }

        // Get classes with their exam schedules for an academic year
        get("/academic-year/{academicYearId}/classes-with-schedules") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classesWithSchedules = examScheduleService.getClassesWithExamSchedules(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classesWithSchedules
            ))
        }

        // Remove all schedules for a specific exam
        delete("/exam/{examId}/all") {
            val examId = call.parameters["examId"]
                ?: throw ApiException("Exam ID is required", HttpStatusCode.BadRequest)

            val deletedCount = examScheduleService.removeAllSchedulesForExam(examId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All schedules for the exam have been removed"
            ))
        }

        // Remove all schedules for a specific class
        delete("/class/{classId}/all") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = examScheduleService.removeAllSchedulesForClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "All schedules for the class have been removed"
            ))
        }
    }
}