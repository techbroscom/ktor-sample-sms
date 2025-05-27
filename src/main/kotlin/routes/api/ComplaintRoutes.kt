package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.ComplaintService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.complaintRoutes(complaintService: ComplaintService) {
    route("/api/v1/complaints") {

        // Get all complaints
        get {
            val complaints = complaintService.getAllComplaints()
            call.respond(ApiResponse(
                success = true,
                data = complaints
            ))
        }

        // Get complaints by author
        get("/author/{authorId}") {
            val authorId = call.parameters["authorId"]
                ?: throw ApiException("Author ID is required", HttpStatusCode.BadRequest)

            val complaints = complaintService.getComplaintsByAuthor(authorId)
            call.respond(ApiResponse(
                success = true,
                data = complaints
            ))
        }

        // Get complaints by category
        get("/category/{category}") {
            val category = call.parameters["category"]
                ?: throw ApiException("Category is required", HttpStatusCode.BadRequest)

            val complaints = complaintService.getComplaintsByCategory(category)
            call.respond(ApiResponse(
                success = true,
                data = complaints
            ))
        }

        // Get complaints by status
        get("/status/{status}") {
            val status = call.parameters["status"]
                ?: throw ApiException("Status is required", HttpStatusCode.BadRequest)

            val complaints = complaintService.getComplaintsByStatus(status)
            call.respond(ApiResponse(
                success = true,
                data = complaints
            ))
        }

        // Get complaint by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Complaint ID is required", HttpStatusCode.BadRequest)

            val complaint = complaintService.getComplaintById(id)
            call.respond(ApiResponse(
                success = true,
                data = complaint
            ))
        }

        // Create complaint
        post {
            val request = call.receive<CreateComplaintRequest>()
            val complaint = complaintService.createComplaint(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = complaint,
                message = "Complaint created successfully"
            ))
        }

        // Update complaint
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Complaint ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateComplaintRequest>()
            val complaint = complaintService.updateComplaint(id, request)
            call.respond(ApiResponse(
                success = true,
                data = complaint,
                message = "Complaint updated successfully"
            ))
        }

        // Update complaint status
        patch("/{id}/status") {
            val id = call.parameters["id"]
                ?: throw ApiException("Complaint ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateStatusRequest>()
            val complaint = complaintService.updateStatus(id, request)
            call.respond(ApiResponse(
                success = true,
                data = complaint,
                message = "Complaint status updated successfully"
            ))
        }

        // Add comment to complaint
        post("/{id}/comments") {
            val id = call.parameters["id"]
                ?: throw ApiException("Complaint ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<AddCommentRequest>()
            val complaint = complaintService.addComment(id, request)
            call.respond(ApiResponse(
                success = true,
                data = complaint,
                message = "Comment added successfully"
            ))
        }

        // Delete complaint
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Complaint ID is required", HttpStatusCode.BadRequest)

            complaintService.deleteComplaint(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Complaint deleted successfully"
            ))
        }
    }
}