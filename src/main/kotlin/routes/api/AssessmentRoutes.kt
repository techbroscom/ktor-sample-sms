package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.AssessmentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.assessmentRoutes(assessmentService: AssessmentService) {

    // ============================================
    // Assessment CRUD (Admin)
    // ============================================

    route("/api/v1/assessments") {

        post {
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receive<CreateAssessmentRequest>()
            val assessment = assessmentService.createAssessment(request, userId)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = assessment, message = "Assessment created"
            ))
        }

        get {
            val accessType = call.request.queryParameters["accessType"]
            val status = call.request.queryParameters["status"]
            val courseId = call.request.queryParameters["courseId"]
            val assessments = assessmentService.getAllAssessments(accessType, status, courseId)
            call.respond(ApiResponse(success = true, data = assessments))
        }

        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val assessment = assessmentService.getAssessment(id)
            call.respond(ApiResponse(success = true, data = assessment))
        }

        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateAssessmentRequest>()
            val assessment = assessmentService.updateAssessment(id, request)
            call.respond(ApiResponse(success = true, data = assessment, message = "Assessment updated"))
        }

        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            assessmentService.deleteAssessment(id)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Assessment deleted"))
        }

        // ============================================
        // Questions
        // ============================================

        post("/{id}/questions") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<CreateQuestionRequest>()
            val question = assessmentService.addQuestion(id, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = question, message = "Question added"
            ))
        }

        get("/{id}/questions") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val questions = assessmentService.getQuestions(id, includeAnswers = true)
            call.respond(ApiResponse(success = true, data = questions))
        }

        put("/{id}/questions/{qId}") {
            val qId = call.parameters["qId"]
                ?: throw ApiException("Question ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateQuestionRequest>()
            assessmentService.updateQuestion(qId, request)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Question updated"))
        }

        delete("/{id}/questions/{qId}") {
            val qId = call.parameters["qId"]
                ?: throw ApiException("Question ID is required", HttpStatusCode.BadRequest)
            assessmentService.deleteQuestion(qId)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Question deleted"))
        }

        // ============================================
        // Batch Enablement (Admin enables for specific batch)
        // ============================================

        post("/{id}/batches/{batchId}/toggle") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val batchId = call.parameters["batchId"]
                ?: throw ApiException("Batch ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receive<ToggleEnablementRequest>()
            val enablement = assessmentService.toggleEnablement(id, batchId, request.enabled, userId)
            val msg = if (request.enabled) "Assessment enabled for batch" else "Assessment disabled for batch"
            call.respond(ApiResponse(success = true, data = enablement, message = msg))
        }

        get("/{id}/enablements") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val enablements = assessmentService.getEnablements(id)
            call.respond(ApiResponse(success = true, data = enablements))
        }

        // ============================================
        // Assignment (Admin assigns to users)
        // ============================================

        post("/{id}/assign") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receive<AssignAssessmentRequest>()
            val assignments = assessmentService.assignAssessment(id, request, userId)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = assignments, message = "Assessment assigned"
            ))
        }

        get("/{id}/assignments") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val assignments = assessmentService.getAssignments(id)
            call.respond(ApiResponse(success = true, data = assignments))
        }

        // ============================================
        // Analytics (Admin)
        // ============================================

        get("/{id}/analytics") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val analytics = assessmentService.getAnalytics(id)
            call.respond(ApiResponse(success = true, data = analytics))
        }

        get("/{id}/attempts") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val attempts = assessmentService.getAttemptsByAssessment(id)
            call.respond(ApiResponse(success = true, data = attempts))
        }

        // ============================================
        // Purchase (Student buys PURCHASABLE assessment)
        // ============================================

        post("/{id}/purchase") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val purchase = assessmentService.purchaseAssessment(id, userId)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = purchase, message = "Assessment purchased"
            ))
        }

        // ============================================
        // Attempt Flow (Student)
        // ============================================

        post("/{id}/start") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val attempt = assessmentService.startAttempt(id, userId)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = attempt, message = "Attempt started"
            ))
        }

        get("/{id}/take") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val questions = assessmentService.getQuestionsForAttempt(id, userId)
            call.respond(ApiResponse(success = true, data = questions))
        }

        post("/{id}/attempts/{attemptId}/submit") {
            val id = call.parameters["id"]
                ?: throw ApiException("Assessment ID is required", HttpStatusCode.BadRequest)
            val attemptId = call.parameters["attemptId"]
                ?: throw ApiException("Attempt ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receive<SubmitAttemptRequest>()
            val result = assessmentService.submitAttempt(id, attemptId, userId, request)
            call.respond(ApiResponse(success = true, data = result, message = "Assessment submitted"))
        }
    }

    // ============================================
    // Student: My Assessments
    // ============================================

    route("/api/v1/assessments/my") {
        get {
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val assessments = assessmentService.getMyAssessments(userId)
            call.respond(ApiResponse(success = true, data = assessments))
        }

        get("/attempts") {
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val attempts = assessmentService.getMyAttempts(userId)
            call.respond(ApiResponse(success = true, data = attempts))
        }

        get("/assignments") {
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val assignments = assessmentService.getMyAssignments(userId)
            call.respond(ApiResponse(success = true, data = assignments))
        }
    }
}
