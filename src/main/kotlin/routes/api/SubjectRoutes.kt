package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateSubjectRequest
import com.example.models.dto.UpdateSubjectRequest
import com.example.models.responses.ApiResponse
import com.example.models.responses.PaginatedResponse
import com.example.services.SubjectService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.subjectRoutes(subjectService: SubjectService) {
    route("/api/v1/subjects") {

        // Get all subjects
        get {
            val subjects = subjectService.getAllSubjects()
            call.respond(ApiResponse(
                success = true,
                data = subjects
            ))
        }

        // Get subjects with pagination
        get("/paginated") {
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 10

            val (subjects, totalCount) = subjectService.getSubjectsPaginated(page, pageSize)
            val totalPages = (totalCount + pageSize - 1) / pageSize

            call.respond(PaginatedResponse(
                success = true,
                data = subjects,
                pagination = PaginatedResponse.PaginationInfo(
                    page = page,
                    pageSize = pageSize,
                    totalItems = totalCount,
                    totalPages = totalPages,
                    hasNext = page < totalPages,
                    hasPrevious = page > 1
                )
            ))
        }

        // Search subjects by name
        get("/search") {
            val name = call.request.queryParameters["name"]
                ?: throw ApiException("name parameter is required", HttpStatusCode.BadRequest)

            val subjects = subjectService.searchSubjectsByName(name)
            call.respond(ApiResponse(
                success = true,
                data = subjects
            ))
        }

        // Get subject by code
        get("/code/{code}") {
            val code = call.parameters["code"]
                ?: throw ApiException("Subject code is required", HttpStatusCode.BadRequest)

            val subject = subjectService.getSubjectByCode(code)
            call.respond(ApiResponse(
                success = true,
                data = subject
            ))
        }

        // Get subject by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val subject = subjectService.getSubjectById(id)
            call.respond(ApiResponse(
                success = true,
                data = subject
            ))
        }

        // Create subject
        post {
            val request = call.receive<CreateSubjectRequest>()
            val subject = subjectService.createSubject(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = subject,
                message = "Subject created successfully"
            ))
        }

        // Update subject
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateSubjectRequest>()
            val subject = subjectService.updateSubject(id, request)
            call.respond(ApiResponse(
                success = true,
                data = subject,
                message = "Subject updated successfully"
            ))
        }

        // Delete subject
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            subjectService.deleteSubject(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Subject deleted successfully"
            ))
        }
    }
}