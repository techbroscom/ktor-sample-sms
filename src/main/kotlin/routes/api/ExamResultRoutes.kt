// Complete API Routes for Exam Results
package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateExamResultRequest
import com.example.models.dto.CreateExamResultRequest
import com.example.models.dto.UpdateExamResultRequest
import com.example.models.responses.ApiResponse
import com.example.services.ExamResultService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.examResultRoutes(examResultService: ExamResultService) {
    route("/api/v1/exam-results") {
        // Get all exam results
        get {
            val examResults = examResultService.getAllExamResults()
            call.respond(ApiResponse(
                success = true,
                data = examResults
            ))
        }

        // Get specific exam result by ID
        get("/{id}") {
            val id = call.parameters["id"] ?: throw ApiException("Result ID is required", HttpStatusCode.BadRequest)
            val examResult = examResultService.getExamResultById(id)
            call.respond(ApiResponse(
                success = true,
                data = examResult
            ))
        }

        // Create new exam result
        post {
            val request = call.receive<CreateExamResultRequest>()
            val examResult = examResultService.createExamResult(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = examResult,
                message = "Exam result created successfully"
            ))
        }

        // Bulk create exam results
        post("/bulk") {
            val request = call.receive<BulkCreateExamResultRequest>()
            val examResults = examResultService.bulkCreateExamResults(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = examResults,
                message = "${examResults.size} exam results created successfully"
            ))
        }

        // Update exam result
        put("/{id}") {
            val id = call.parameters["id"] ?: throw ApiException("Result ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateExamResultRequest>()
            val examResult = examResultService.updateExamResult(id, request)
            call.respond(ApiResponse(
                success = true,
                data = examResult,
                message = "Exam result updated successfully"
            ))
        }

        // Delete exam result
        delete("/{id}") {
            val id = call.parameters["id"] ?: throw ApiException("Result ID is required", HttpStatusCode.BadRequest)
            examResultService.deleteExamResult(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Exam result deleted successfully"
            ))
        }

        // Get results by exam ID
        get("/exam/{examId}") {
            val examId = call.parameters["examId"] ?: throw ApiException("Exam ID is required", HttpStatusCode.BadRequest)
            val examResults = examResultService.getResultsByExam(examId)
            call.respond(ApiResponse(
                success = true,
                data = examResults
            ))
        }

        // Get results by student ID
        get("/student/{studentId}") {
            val studentId = call.parameters["studentId"] ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)
            val examResults = examResultService.getResultsByStudent(studentId)
            call.respond(ApiResponse(
                success = true,
                data = examResults
            ))
        }

        // Get results by class and exam
        get("/class/{classId}/exam/{examId}") {
            val classId = call.parameters["classId"] ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val examId = call.parameters["examId"] ?: throw ApiException("Exam ID is required", HttpStatusCode.BadRequest)
            val examResults = examResultService.getResultsByClassAndExam(classId, examId)
            call.respond(ApiResponse(
                success = true,
                data = examResults
            ))
        }

        // Get class result summary
        get("/summary/class/{classId}/exam/{examId}") {
            val classId = call.parameters["classId"] ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val examId = call.parameters["examId"] ?: throw ApiException("Exam ID is required", HttpStatusCode.BadRequest)
            val summary = examResultService.getClassResultSummary(classId, examId)
            call.respond(ApiResponse(
                success = true,
                data = summary
            ))
        }

        // Delete all results for an exam
        delete("/exam/{examId}/all") {
            val examId = call.parameters["examId"] ?: throw ApiException("Exam ID is required", HttpStatusCode.BadRequest)
            val deletedCount = examResultService.removeAllResultsForExam(examId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "$deletedCount exam results deleted successfully"
            ))
        }

        // Delete all results for a student
        delete("/student/{studentId}/all") {
            val studentId = call.parameters["studentId"] ?: throw ApiException("Student ID is required", HttpStatusCode.BadRequest)
            val deletedCount = examResultService.removeAllResultsForStudent(studentId)
            call.respond(ApiResponse(
                success = true,
                data = mapOf("deletedCount" to deletedCount),
                message = "$deletedCount exam results deleted successfully"
            ))
        }
    }

    // Additional routes for reporting and analytics
    route("/api/v1/reports") {
        // Get exams with all their results for an academic year
        get("/exams-with-results/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)
            val examsWithResults = examResultService.getExamsWithResults(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = examsWithResults
            ))
        }

        // Get students with all their exam results for an academic year
        get("/students-with-results/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)
            val studentsWithResults = examResultService.getStudentsWithExamResults(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = studentsWithResults
            ))
        }
    }
}