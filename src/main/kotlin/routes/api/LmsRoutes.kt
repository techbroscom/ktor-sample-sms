package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.LmsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.lmsRoutes(lmsService: LmsService) {

    // ============================================
    // Public Catalog
    // ============================================

    route("/api/v1/lms/catalog") {
        get {
            val catalog = lmsService.getCatalog()
            call.respond(ApiResponse(success = true, data = catalog))
        }
    }

    // ============================================
    // Course Management (Admin)
    // ============================================

    route("/api/v1/lms/courses") {

        post {
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receive<CreateCourseRequest>()
            val course = lmsService.createCourse(request, userId)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = course, message = "Course created successfully"
            ))
        }

        get {
            val status = call.request.queryParameters["status"]
            val courses = lmsService.getAllCourses(status)
            call.respond(ApiResponse(success = true, data = courses))
        }

        get("/{id}") {
            val courseId = call.parameters["id"]
                ?: throw ApiException("Course ID is required", HttpStatusCode.BadRequest)
            val course = lmsService.getCourse(courseId)
            call.respond(ApiResponse(success = true, data = course))
        }

        put("/{id}") {
            val courseId = call.parameters["id"]
                ?: throw ApiException("Course ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateCourseRequest>()
            val course = lmsService.updateCourse(courseId, request)
            call.respond(ApiResponse(success = true, data = course, message = "Course updated"))
        }

        delete("/{id}") {
            val courseId = call.parameters["id"]
                ?: throw ApiException("Course ID is required", HttpStatusCode.BadRequest)
            lmsService.deleteCourse(courseId)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Course deleted"))
        }

        // Sections
        post("/{id}/sections") {
            val courseId = call.parameters["id"]
                ?: throw ApiException("Course ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<CreateSectionRequest>()
            val course = lmsService.addSection(courseId, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = course, message = "Section added"
            ))
        }

        // Batches for a course
        post("/{id}/batches") {
            val courseId = call.parameters["id"]
                ?: throw ApiException("Course ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receive<CreateBatchRequest>()
            val batch = lmsService.createBatch(courseId, request, userId)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = batch, message = "Batch created"
            ))
        }

        get("/{id}/batches") {
            val courseId = call.parameters["id"]
                ?: throw ApiException("Course ID is required", HttpStatusCode.BadRequest)
            val batches = lmsService.getBatchesByCourse(courseId)
            call.respond(ApiResponse(success = true, data = batches))
        }
    }

    // ============================================
    // Section Management
    // ============================================

    route("/api/v1/lms/sections") {
        put("/{id}") {
            val sectionId = call.parameters["id"]
                ?: throw ApiException("Section ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateSectionRequest>()
            lmsService.updateSection(sectionId, request)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Section updated"))
        }

        delete("/{id}") {
            val sectionId = call.parameters["id"]
                ?: throw ApiException("Section ID is required", HttpStatusCode.BadRequest)
            lmsService.deleteSection(sectionId)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Section deleted"))
        }

        // Session templates for a section
        post("/{id}/session-templates") {
            val sectionId = call.parameters["id"]
                ?: throw ApiException("Section ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<CreateSessionTemplateRequest>()
            val templateId = lmsService.addSessionTemplate(sectionId, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = mapOf("id" to templateId.toString()),
                message = "Session template added"
            ))
        }
    }

    // ============================================
    // Session Template Management
    // ============================================

    route("/api/v1/lms/session-templates") {
        put("/{id}") {
            val templateId = call.parameters["id"]
                ?: throw ApiException("Template ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateSessionTemplateRequest>()
            lmsService.updateSessionTemplate(templateId, request)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Template updated"))
        }

        delete("/{id}") {
            val templateId = call.parameters["id"]
                ?: throw ApiException("Template ID is required", HttpStatusCode.BadRequest)
            lmsService.deleteSessionTemplate(templateId)
            call.respond(ApiResponse<Unit>(success = true, data = null, message = "Template deleted"))
        }
    }

    // ============================================
    // Batch Management
    // ============================================

    route("/api/v1/lms/batches") {
        get("/{id}") {
            val batchId = call.parameters["id"]
                ?: throw ApiException("Batch ID is required", HttpStatusCode.BadRequest)
            val batch = lmsService.getBatch(batchId)
            call.respond(ApiResponse(success = true, data = batch))
        }

        put("/{id}") {
            val batchId = call.parameters["id"]
                ?: throw ApiException("Batch ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateBatchRequest>()
            val batch = lmsService.updateBatch(batchId, request)
            call.respond(ApiResponse(success = true, data = batch, message = "Batch updated"))
        }

        // Schedule sessions for a batch
        post("/{id}/sessions") {
            val batchId = call.parameters["id"]
                ?: throw ApiException("Batch ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<CreateBatchSessionRequest>()
            val session = lmsService.addBatchSession(batchId, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = session, message = "Session scheduled"
            ))
        }

        // Purchase full batch
        post("/{id}/purchase") {
            val batchId = call.parameters["id"]
                ?: throw ApiException("Batch ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receiveNullable<PurchaseBatchRequest>() ?: PurchaseBatchRequest()
            val enrollment = lmsService.purchaseBatch(batchId, userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = enrollment, message = "Enrolled successfully"
            ))
        }

        // Purchase a specific section in a batch
        post("/{id}/sections/{sectionId}/purchase") {
            val batchId = call.parameters["id"]
                ?: throw ApiException("Batch ID is required", HttpStatusCode.BadRequest)
            val sectionId = call.parameters["sectionId"]
                ?: throw ApiException("Section ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val request = call.receiveNullable<PurchaseSectionRequest>()
                ?: PurchaseSectionRequest(sectionId = sectionId)
            val enrollment = lmsService.purchaseSection(batchId, sectionId, userId, request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true, data = enrollment, message = "Enrolled in section successfully"
            ))
        }
    }

    // ============================================
    // Batch Session Management
    // ============================================

    route("/api/v1/lms/batch-sessions") {
        put("/{id}") {
            val sessionId = call.parameters["id"]
                ?: throw ApiException("Session ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateBatchSessionRequest>()
            val session = lmsService.updateBatchSession(sessionId, request)
            call.respond(ApiResponse(success = true, data = session, message = "Session updated"))
        }

        // Join session (get meeting link)
        get("/{id}/join") {
            val sessionId = call.parameters["id"]
                ?: throw ApiException("Session ID is required", HttpStatusCode.BadRequest)
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val joinResponse = lmsService.joinSession(sessionId, userId)
            call.respond(ApiResponse(success = true, data = joinResponse))
        }
    }

    // ============================================
    // My Learning
    // ============================================

    route("/api/v1/lms/my-courses") {
        get {
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)
            val courses = lmsService.getMyEnrolledCourses(userId)
            call.respond(ApiResponse(success = true, data = courses))
        }
    }

    // ============================================
    // LMS Config (Admin)
    // ============================================

    route("/api/v1/lms/config") {
        get {
            val config = lmsService.getConfig()
            call.respond(ApiResponse(success = true, data = config))
        }

        put {
            val request = call.receive<UpdateLmsConfigRequest>()
            val config = lmsService.updateConfig(request)
            call.respond(ApiResponse(
                success = true, data = config, message = "LMS config updated"
            ))
        }
    }
}
