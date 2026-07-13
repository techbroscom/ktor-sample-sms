package com.example.models.dto

import kotlinx.serialization.Serializable

// ============================================
// Course DTOs
// ============================================

@Serializable
data class CourseDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val instructor: String,
    val thumbnail: String? = null,
    val category: String,
    val totalDuration: String? = null,
    val status: String,
    val sections: List<SectionDto> = emptyList(),
    val activeBatches: List<BatchSummaryDto> = emptyList(),
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CourseSummaryDto(
    val id: String,
    val title: String,
    val instructor: String,
    val thumbnail: String? = null,
    val category: String,
    val totalDuration: String? = null,
    val status: String,
    val sectionCount: Int,
    val nextBatchStartDate: String? = null,
    val lowestPrice: String? = null
)

@Serializable
data class CreateCourseRequest(
    val title: String,
    val description: String? = null,
    val instructor: String,
    val thumbnail: String? = null,
    val category: String,
    val totalDuration: String? = null
)

@Serializable
data class UpdateCourseRequest(
    val title: String? = null,
    val description: String? = null,
    val instructor: String? = null,
    val thumbnail: String? = null,
    val category: String? = null,
    val totalDuration: String? = null,
    val status: String? = null
)

// ============================================
// Section DTOs
// ============================================

@Serializable
data class SectionDto(
    val id: String,
    val courseId: String,
    val title: String,
    val description: String? = null,
    val order: Int,
    val sessionTemplates: List<SessionTemplateDto> = emptyList(),
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateSectionRequest(
    val title: String,
    val description: String? = null,
    val order: Int
)

@Serializable
data class UpdateSectionRequest(
    val title: String? = null,
    val description: String? = null,
    val order: Int? = null
)

// ============================================
// Session Template DTOs
// ============================================

@Serializable
data class SessionTemplateDto(
    val id: String,
    val sectionId: String,
    val title: String,
    val description: String? = null,
    val order: Int,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateSessionTemplateRequest(
    val title: String,
    val description: String? = null,
    val order: Int
)

@Serializable
data class UpdateSessionTemplateRequest(
    val title: String? = null,
    val description: String? = null,
    val order: Int? = null
)

// ============================================
// Batch DTOs
// ============================================

@Serializable
data class BatchDto(
    val id: String,
    val courseId: String,
    val courseName: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val price: String,
    val currency: String,
    val maxSeats: Int? = null,
    val enrolledCount: Int,
    val availableSeats: Int? = null,
    val status: String,
    val sections: List<BatchSectionDto> = emptyList(),
    val sessions: List<BatchSessionDto> = emptyList(),
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class BatchSummaryDto(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val price: String,
    val currency: String,
    val maxSeats: Int? = null,
    val enrolledCount: Int,
    val availableSeats: Int? = null,
    val status: String
)

@Serializable
data class BatchSectionDto(
    val id: String,
    val sectionId: String,
    val sectionTitle: String,
    val price: String
)

@Serializable
data class CreateBatchRequest(
    val name: String,
    val startDate: String,
    val endDate: String,
    val price: String,
    val currency: String = "INR",
    val maxSeats: Int? = null,
    val sectionPricing: List<SectionPricingRequest> = emptyList()
)

@Serializable
data class SectionPricingRequest(
    val sectionId: String,
    val price: String
)

@Serializable
data class UpdateBatchRequest(
    val name: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val price: String? = null,
    val currency: String? = null,
    val maxSeats: Int? = null,
    val status: String? = null
)

// ============================================
// Batch Session DTOs
// ============================================

@Serializable
data class BatchSessionDto(
    val id: String,
    val batchId: String,
    val sectionId: String,
    val sectionTitle: String,
    val sessionTemplateId: String,
    val title: String,
    val description: String? = null,
    val scheduledDate: String,
    val startTime: String,
    val endTime: String,
    val meetingLink: String? = null, // Only visible if user is enrolled + within time window
    val status: String,
    val order: Int,
    val canJoin: Boolean = false, // Computed: is within join window?
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateBatchSessionRequest(
    val sectionId: String,
    val sessionTemplateId: String,
    val title: String? = null, // Override template title
    val description: String? = null,
    val scheduledDate: String,
    val startTime: String,
    val endTime: String,
    val meetingLink: String? = null,
    val order: Int
)

@Serializable
data class UpdateBatchSessionRequest(
    val title: String? = null,
    val description: String? = null,
    val scheduledDate: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val meetingLink: String? = null,
    val status: String? = null,
    val order: Int? = null
)

// ============================================
// Enrollment DTOs
// ============================================

@Serializable
data class EnrollmentDto(
    val id: String,
    val userId: String,
    val userName: String,
    val batchId: String,
    val batchName: String,
    val courseName: String,
    val purchaseType: String,
    val sectionId: String? = null,
    val sectionTitle: String? = null,
    val amount: String,
    val currency: String,
    val paymentStatus: String,
    val paymentReference: String? = null,
    val paymentProvider: String? = null,
    val purchaseDate: String,
    val createdAt: String
)

@Serializable
data class PurchaseBatchRequest(
    val paymentReference: String? = null // For future PG integration
)

@Serializable
data class PurchaseSectionRequest(
    val sectionId: String,
    val paymentReference: String? = null
)

@Serializable
data class MyEnrolledCourseDto(
    val enrollmentId: String,
    val courseId: String,
    val courseName: String,
    val batchId: String,
    val batchName: String,
    val purchaseType: String,
    val sectionId: String? = null,
    val sectionTitle: String? = null,
    val batchStartDate: String,
    val batchEndDate: String,
    val batchStatus: String,
    val upcomingSessions: List<BatchSessionDto> = emptyList(),
    val purchaseDate: String
)

@Serializable
data class SessionJoinResponse(
    val sessionId: String,
    val title: String,
    val meetingLink: String,
    val startTime: String,
    val endTime: String,
    val provider: String // ZOOM / GOOGLE_MEET / CUSTOM_LINK
)

// ============================================
// LMS Config DTOs
// ============================================

@Serializable
data class LmsConfigDto(
    val id: String,
    val meetingProvider: String,
    val paymentProvider: String,
    val currency: String,
    val paymentEnabled: Boolean,
    val notificationsEnabled: Boolean,
    val sessionJoinWindowMinutes: Int,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class UpdateLmsConfigRequest(
    val meetingProvider: String? = null,
    val meetingCredentials: String? = null, // JSON string (encrypted on backend)
    val paymentProvider: String? = null,
    val paymentCredentials: String? = null, // JSON string (encrypted on backend)
    val currency: String? = null,
    val paymentEnabled: Boolean? = null,
    val notificationsEnabled: Boolean? = null,
    val sessionJoinWindowMinutes: Int? = null
)

// ============================================
// Catalog DTOs (Public browsing)
// ============================================

@Serializable
data class CatalogCourseDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val instructor: String,
    val thumbnail: String? = null,
    val category: String,
    val totalDuration: String? = null,
    val sectionCount: Int,
    val batches: List<CatalogBatchDto> = emptyList()
)

@Serializable
data class CatalogBatchDto(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val price: String,
    val currency: String,
    val maxSeats: Int? = null,
    val enrolledCount: Int,
    val availableSeats: Int? = null,
    val status: String,
    val sectionPricing: List<CatalogSectionPricingDto> = emptyList()
)

@Serializable
data class CatalogSectionPricingDto(
    val sectionId: String,
    val sectionTitle: String,
    val price: String
)
