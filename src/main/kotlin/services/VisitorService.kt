package com.example.services

import com.example.database.tables.VisitorStatus
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.PaginatedResponse
import com.example.repositories.UserRepository
import com.example.repositories.VisitorRepository
import com.example.tenant.TenantContextHolder
import io.ktor.http.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class VisitorService(
    private val visitorRepository: VisitorRepository,
    private val userRepository: UserRepository
) {

    suspend fun createVisitor(request: CreateVisitorRequest, createdBy: String): VisitorDto {
        val tenantContext = TenantContextHolder.getTenant()
            ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

        validateCreateVisitorRequest(request)

        // Validate host user exists
        val hostUserId = try {
            UUID.fromString(request.hostUserId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid host user ID format", HttpStatusCode.BadRequest)
        }

        val hostUser = userRepository.findById(hostUserId)
            ?: throw ApiException("Host user not found", HttpStatusCode.NotFound)

        // Validate host is ADMIN or STAFF
        if (hostUser.role !in listOf("ADMIN", "STAFF")) {
            throw ApiException("Host must be an admin or staff member", HttpStatusCode.BadRequest)
        }

        // Parse and validate dates
        val visitDate = try {
            LocalDate.parse(request.visitDate)
        } catch (e: Exception) {
            throw ApiException("Invalid visit date format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
        }

        val expectedCheckInTime = try {
            LocalDateTime.parse(request.expectedCheckInTime)
        } catch (e: Exception) {
            throw ApiException("Invalid check-in time format. Use ISO 8601 datetime", HttpStatusCode.BadRequest)
        }

        if (visitDate.isBefore(LocalDate.now())) {
            throw ApiException("Visit date cannot be in the past", HttpStatusCode.BadRequest)
        }

        // Create visitor
        val createdByUUID = try {
            UUID.fromString(createdBy)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid creator ID format", HttpStatusCode.BadRequest)
        }

        val visitorId = visitorRepository.create(request, createdByUUID)

        return visitorRepository.findById(visitorId)
            ?: throw ApiException("Failed to retrieve created visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun getVisitor(visitorId: String): VisitorDto {
        val id = try {
            UUID.fromString(visitorId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid visitor ID format", HttpStatusCode.BadRequest)
        }

        return visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)
    }

    suspend fun searchVisitors(request: VisitorSearchRequest): PaginatedResponse<List<VisitorDto>> {
        // Validate status if provided
        request.status?.let {
            try {
                VisitorStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw ApiException("Invalid visitor status: $it", HttpStatusCode.BadRequest)
            }
        }

        val (visitors, total) = visitorRepository.search(request)

        val totalPages = ((total + request.pageSize - 1) / request.pageSize)

        return PaginatedResponse(
            success = true,
            data = visitors,
            pagination = PaginatedResponse.PaginationInfo(
                page = request.page,
                pageSize = request.pageSize,
                totalItems = total,
                totalPages = totalPages
            )
        )
    }

    suspend fun checkIn(visitorId: String, request: CheckInRequest): VisitorDto {
        val id = try {
            UUID.fromString(visitorId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid visitor ID format", HttpStatusCode.BadRequest)
        }

        // Validate visitor exists and is in SCHEDULED status
        val visitor = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        if (visitor.status != VisitorStatus.SCHEDULED.name) {
            throw ApiException(
                "Only scheduled visitors can be checked in. Current status: ${visitor.status}",
                HttpStatusCode.BadRequest
            )
        }

        val actualTime = if (request.actualCheckInTime != null) {
            try {
                LocalDateTime.parse(request.actualCheckInTime)
            } catch (e: Exception) {
                throw ApiException("Invalid check-in time format. Use ISO 8601 datetime", HttpStatusCode.BadRequest)
            }
        } else {
            LocalDateTime.now()
        }

        val success = visitorRepository.checkIn(id, actualTime)
        if (!success) {
            throw ApiException("Failed to check in visitor", HttpStatusCode.InternalServerError)
        }

        return visitorRepository.findById(id)
            ?: throw ApiException("Failed to retrieve updated visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun checkOut(visitorId: String, request: CheckOutRequest): VisitorDto {
        val id = try {
            UUID.fromString(visitorId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid visitor ID format", HttpStatusCode.BadRequest)
        }

        // Validate visitor exists and is checked in
        val visitor = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        if (visitor.status != VisitorStatus.CHECKED_IN.name) {
            throw ApiException(
                "Only checked-in visitors can be checked out. Current status: ${visitor.status}",
                HttpStatusCode.BadRequest
            )
        }

        val checkOutTime = if (request.checkOutTime != null) {
            try {
                LocalDateTime.parse(request.checkOutTime)
            } catch (e: Exception) {
                throw ApiException("Invalid check-out time format. Use ISO 8601 datetime", HttpStatusCode.BadRequest)
            }
        } else {
            LocalDateTime.now()
        }

        val success = visitorRepository.checkOut(id, checkOutTime, request.notes)
        if (!success) {
            throw ApiException("Failed to check out visitor", HttpStatusCode.InternalServerError)
        }

        return visitorRepository.findById(id)
            ?: throw ApiException("Failed to retrieve updated visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun updateVisitor(visitorId: String, request: UpdateVisitorRequest): VisitorDto {
        val id = try {
            UUID.fromString(visitorId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid visitor ID format", HttpStatusCode.BadRequest)
        }

        // Validate visitor exists
        val existing = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        // Can't update checked-out visitors
        if (existing.status == VisitorStatus.CHECKED_OUT.name) {
            throw ApiException("Cannot update checked-out visitors", HttpStatusCode.BadRequest)
        }

        // Validate new host if provided
        request.hostUserId?.let { hostId ->
            val hostUserId = try {
                UUID.fromString(hostId)
            } catch (e: IllegalArgumentException) {
                throw ApiException("Invalid host user ID format", HttpStatusCode.BadRequest)
            }

            val hostUser = userRepository.findById(hostUserId)
                ?: throw ApiException("Host user not found", HttpStatusCode.NotFound)

            if (hostUser.role !in listOf("ADMIN", "STAFF")) {
                throw ApiException("Host must be an admin or staff member", HttpStatusCode.BadRequest)
            }
        }

        // Validate dates if provided
        request.visitDate?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw ApiException("Invalid visit date format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
            }
        }

        request.expectedCheckInTime?.let {
            try {
                LocalDateTime.parse(it)
            } catch (e: Exception) {
                throw ApiException("Invalid check-in time format. Use ISO 8601 datetime", HttpStatusCode.BadRequest)
            }
        }

        // Validate mobile number if provided
        request.mobileNumber?.let {
            if (!it.matches(Regex("^\\+?[1-9]\\d{1,14}$"))) {
                throw ApiException("Invalid mobile number format", HttpStatusCode.BadRequest)
            }
        }

        val success = visitorRepository.update(id, request)
        if (!success) {
            throw ApiException("Failed to update visitor", HttpStatusCode.InternalServerError)
        }

        return visitorRepository.findById(id)
            ?: throw ApiException("Failed to retrieve updated visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun deleteVisitor(visitorId: String) {
        val id = try {
            UUID.fromString(visitorId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid visitor ID format", HttpStatusCode.BadRequest)
        }

        val visitor = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        // Only allow deletion of SCHEDULED or CANCELLED visitors
        if (visitor.status !in listOf(VisitorStatus.SCHEDULED.name, VisitorStatus.CANCELLED.name)) {
            throw ApiException(
                "Can only delete scheduled or cancelled visitors",
                HttpStatusCode.BadRequest
            )
        }

        val success = visitorRepository.delete(id)
        if (!success) {
            throw ApiException("Failed to delete visitor", HttpStatusCode.InternalServerError)
        }
    }

    suspend fun cancelVisitor(visitorId: String): VisitorDto {
        val id = try {
            UUID.fromString(visitorId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid visitor ID format", HttpStatusCode.BadRequest)
        }

        val visitor = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        if (visitor.status != VisitorStatus.SCHEDULED.name) {
            throw ApiException(
                "Only scheduled visitors can be cancelled. Current status: ${visitor.status}",
                HttpStatusCode.BadRequest
            )
        }

        val success = visitorRepository.updateStatus(id, VisitorStatus.CANCELLED)
        if (!success) {
            throw ApiException("Failed to cancel visitor", HttpStatusCode.InternalServerError)
        }

        return visitorRepository.findById(id)
            ?: throw ApiException("Failed to retrieve updated visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun getVisitorStats(fromDate: String?, toDate: String?): VisitorStatsDto {
        val from = if (fromDate != null) {
            try {
                LocalDate.parse(fromDate)
            } catch (e: Exception) {
                throw ApiException("Invalid from date format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
            }
        } else {
            LocalDate.now().minusDays(30)
        }

        val to = if (toDate != null) {
            try {
                LocalDate.parse(toDate)
            } catch (e: Exception) {
                throw ApiException("Invalid to date format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
            }
        } else {
            LocalDate.now()
        }

        if (from.isAfter(to)) {
            throw ApiException("From date cannot be after to date", HttpStatusCode.BadRequest)
        }

        return visitorRepository.getStats(from, to)
    }

    suspend fun getMyHostedVisitors(hostUserId: String, status: String?): List<VisitorDto> {
        val hostId = try {
            UUID.fromString(hostUserId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid host user ID format", HttpStatusCode.BadRequest)
        }

        // Validate status if provided
        status?.let {
            try {
                VisitorStatus.valueOf(it)
            } catch (e: IllegalArgumentException) {
                throw ApiException("Invalid visitor status: $it", HttpStatusCode.BadRequest)
            }
        }

        return visitorRepository.findByHostUserId(hostId, status)
    }

    private fun validateCreateVisitorRequest(request: CreateVisitorRequest) {
        if (request.firstName.isBlank()) {
            throw ApiException("First name is required", HttpStatusCode.BadRequest)
        }
        if (request.lastName.isBlank()) {
            throw ApiException("Last name is required", HttpStatusCode.BadRequest)
        }
        if (request.mobileNumber.isBlank()) {
            throw ApiException("Mobile number is required", HttpStatusCode.BadRequest)
        }
        if (!request.mobileNumber.matches(Regex("^\\+?[1-9]\\d{1,14}$"))) {
            throw ApiException("Invalid mobile number format", HttpStatusCode.BadRequest)
        }
        if (request.purposeOfVisit.isBlank()) {
            throw ApiException("Purpose of visit is required", HttpStatusCode.BadRequest)
        }
        if (request.visitDate.isBlank()) {
            throw ApiException("Visit date is required", HttpStatusCode.BadRequest)
        }
        if (request.expectedCheckInTime.isBlank()) {
            throw ApiException("Expected check-in time is required", HttpStatusCode.BadRequest)
        }
        if (request.hostUserId.isBlank()) {
            throw ApiException("Host user ID is required", HttpStatusCode.BadRequest)
        }
        request.email?.let {
            if (it.isNotBlank() && !it.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
                throw ApiException("Invalid email format", HttpStatusCode.BadRequest)
            }
        }
    }
}
