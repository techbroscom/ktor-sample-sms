package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.ComplaintRepository
import io.ktor.http.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ComplaintService(
    private val complaintRepository: ComplaintRepository,
    private val notificationService: NotificationService?
) {

    suspend fun createComplaint(request: CreateComplaintRequest): ComplaintDto {
        validateComplaintRequest(request)

        val complaintId = complaintRepository.create(request)
        
        // Send FCM notification to admins about new complaint
        notificationService?.sendComplaintNotification(
            title = "New Complaint: ${request.title}",
            message = "A new complaint has been submitted in category: ${request.category}"
        )
        
        return getComplaintById(complaintId)
    }

    suspend fun getComplaintById(id: String): ComplaintDto {
        if (id.isBlank()) {
            throw ApiException("Complaint ID cannot be empty", HttpStatusCode.BadRequest)
        }

        return complaintRepository.findById(id)
            ?: throw ApiException("Complaint not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllComplaints(): List<ComplaintDto> {
        return complaintRepository.findAll()
    }

    suspend fun getComplaintsByAuthor(authorId: String): List<ComplaintDto> {
        if (authorId.isBlank()) {
            throw ApiException("Author ID cannot be empty", HttpStatusCode.BadRequest)
        }

        return complaintRepository.findByAuthor(authorId)
    }

    suspend fun getComplaintsWithFilters(
        status: String? = null,
        category: String? = null,
        authorId: String? = null,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): com.example.models.responses.PaginatedResponse<List<ComplaintDto>> {
        // Validate pagination parameters
        val validPage = if (page < 1) 1 else page
        val validPageSize = when {
            pageSize < 1 -> 20
            pageSize > 100 -> 100
            else -> pageSize
        }

        // Validate status if provided
        if (status != null) {
            validateStatus(status)
        }

        // Get total count
        val totalItems = complaintRepository.countWithFilters(status, category, authorId, search)
        val totalPages = if (totalItems == 0L) 0L else ((totalItems + validPageSize - 1) / validPageSize)

        // Get paginated data
        val complaints = complaintRepository.findWithFilters(status, category, authorId, search, validPage, validPageSize)

        // Build pagination info
        val paginationInfo = com.example.models.responses.PaginatedResponse.PaginationInfo(
            page = validPage,
            pageSize = validPageSize,
            totalItems = totalItems,
            totalPages = totalPages,
            hasNext = validPage < totalPages,
            hasPrevious = validPage > 1
        )

        return com.example.models.responses.PaginatedResponse(
            success = true,
            data = complaints,
            pagination = paginationInfo
        )
    }

    suspend fun updateComplaint(id: String, request: UpdateComplaintRequest): ComplaintDto {
        validateUpdateComplaintRequest(request)

        val updated = complaintRepository.update(id, request)
        if (!updated) {
            throw ApiException("Complaint not found", HttpStatusCode.NotFound)
        }

        return getComplaintById(id)
    }

    suspend fun deleteComplaint(id: String) {
        val deleted = complaintRepository.delete(id)
        if (!deleted) {
            throw ApiException("Complaint not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun updateStatus(id: String, request: UpdateStatusRequest): ComplaintDto {
        validateStatus(request.status)

        // Get complaint to send notification to author
        val complaint = getComplaintById(id)

        val comment = CommentDto(
            comment = request.comment,
            commentedBy = request.commentedBy,
            commentedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        complaintRepository.addComment(id, comment)
        val updated = complaintRepository.updateStatus(id, request.status)
        if (!updated) {
            throw ApiException("Complaint not found", HttpStatusCode.NotFound)
        }

        // Send FCM notification to the complaint author about status update
        notificationService?.sendComplaintStatusUpdateNotification(
            userId = complaint.author,
            complaintTitle = complaint.title,
            newStatus = request.status
        )

        return getComplaintById(id)
    }

    suspend fun addComment(id: String, request: AddCommentRequest): ComplaintDto {
        validateCommentRequest(request)

        val comment = CommentDto(
            comment = request.comment,
            commentedBy = request.commentedBy,
            commentedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )

        val updated = complaintRepository.addComment(id, comment)
        if (!updated) {
            throw ApiException("Complaint not found", HttpStatusCode.NotFound)
        }

        return getComplaintById(id)
    }

    suspend fun getComplaintsByCategory(category: String): List<ComplaintDto> {
        if (category.isBlank()) {
            throw ApiException("Category cannot be empty", HttpStatusCode.BadRequest)
        }

        return complaintRepository.findByCategory(category)
    }

    suspend fun getComplaintsByStatus(status: String): List<ComplaintDto> {
        validateStatus(status)
        return complaintRepository.findByStatus(status)
    }

    private fun validateComplaintRequest(request: CreateComplaintRequest) {
        when {
            request.title.isBlank() -> throw ApiException("Title cannot be empty", HttpStatusCode.BadRequest)
            request.title.length > 500 -> throw ApiException("Title is too long (max 500 characters)", HttpStatusCode.BadRequest)
            request.content.isBlank() -> throw ApiException("Content cannot be empty", HttpStatusCode.BadRequest)
            request.author.isBlank() -> throw ApiException("Author cannot be empty", HttpStatusCode.BadRequest)
            request.category.isBlank() -> throw ApiException("Category cannot be empty", HttpStatusCode.BadRequest)
            request.category.length > 100 -> throw ApiException("Category is too long (max 100 characters)", HttpStatusCode.BadRequest)
        }
    }

    private fun validateUpdateComplaintRequest(request: UpdateComplaintRequest) {
        when {
            request.title.isBlank() -> throw ApiException("Title cannot be empty", HttpStatusCode.BadRequest)
            request.title.length > 500 -> throw ApiException("Title is too long (max 500 characters)", HttpStatusCode.BadRequest)
            request.content.isBlank() -> throw ApiException("Content cannot be empty", HttpStatusCode.BadRequest)
            request.category.isBlank() -> throw ApiException("Category cannot be empty", HttpStatusCode.BadRequest)
            request.category.length > 100 -> throw ApiException("Category is too long (max 100 characters)", HttpStatusCode.BadRequest)
        }
        validateStatus(request.status)
    }

    private fun validateCommentRequest(request: AddCommentRequest) {
        when {
            request.comment.isBlank() -> throw ApiException("Comment cannot be empty", HttpStatusCode.BadRequest)
            request.commentedBy.isBlank() -> throw ApiException("Commenter ID cannot be empty", HttpStatusCode.BadRequest)
        }
    }

    private fun validateStatus(status: String) {
        val validStatuses = listOf("OPEN","PENDING", "IN_PROGRESS", "RESOLVED", "REJECTED", "CLOSED")
        if (status !in validStatuses) {
            throw ApiException("Invalid status. Valid statuses are: ${validStatuses.joinToString(", ")}", HttpStatusCode.BadRequest)
        }
    }
}