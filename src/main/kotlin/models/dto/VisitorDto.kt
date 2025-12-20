package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class VisitorDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val email: String? = null,
    val mobileNumber: String,
    val organizationName: String? = null,
    val purposeOfVisit: String,
    val visitDate: String,
    val expectedCheckInTime: String,
    val actualCheckInTime: String? = null,
    val checkOutTime: String? = null,
    val status: String,
    val hostUser: UserSummaryDto,
    val identificationProof: String? = null,
    val identificationNumber: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val createdBy: UserSummaryDto
)

@Serializable
data class UserSummaryDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String
)

@Serializable
data class CreateVisitorRequest(
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val mobileNumber: String,
    val organizationName: String? = null,
    val purposeOfVisit: String,
    val visitDate: String, // ISO 8601 date (YYYY-MM-DD)
    val expectedCheckInTime: String, // ISO 8601 datetime
    val hostUserId: String, // UUID string
    val identificationProof: String? = null,
    val identificationNumber: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null
)

@Serializable
data class UpdateVisitorRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val mobileNumber: String? = null,
    val organizationName: String? = null,
    val purposeOfVisit: String? = null,
    val visitDate: String? = null,
    val expectedCheckInTime: String? = null,
    val hostUserId: String? = null,
    val notes: String? = null
)

@Serializable
data class CheckInRequest(
    val actualCheckInTime: String? = null, // Optional, defaults to now
    val photoUrl: String? = null, // Optional check-in photo
    val passNumber: String? = null // If using visitor passes
)

@Serializable
data class CheckOutRequest(
    val checkOutTime: String? = null, // Optional, defaults to now
    val notes: String? = null // Exit notes
)

@Serializable
data class VisitorSearchRequest(
    val searchQuery: String? = null,
    val visitDate: String? = null,
    val status: String? = null,
    val hostUserId: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val page: Int = 1,
    val pageSize: Int = 20
)

@Serializable
data class VisitorStatsDto(
    val totalVisitors: Int,
    val currentlyCheckedIn: Int,
    val scheduledToday: Int,
    val completedToday: Int,
    val noShowsThisWeek: Int
)

@Serializable
data class VisitorPassDto(
    val id: Int,
    val visitorId: String,
    val passNumber: String,
    val qrCodeUrl: String? = null,
    val isActive: Boolean,
    val issuedAt: String,
    val returnedAt: String? = null
)
