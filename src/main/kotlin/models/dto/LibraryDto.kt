package com.example.models.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

// ============================================
// Book DTOs
// ============================================

@Serializable
data class BookDto(
    val id: String,
    val isbn: String? = null,
    val title: String,
    val author: String,
    val publisher: String? = null,
    val publicationYear: Int? = null,
    val edition: String? = null,
    val language: String,
    val category: String,
    val subCategory: String? = null,
    val totalCopies: Int,
    val availableCopies: Int,
    val shelfLocation: String? = null,
    val coverImageUrl: String? = null,
    val description: String? = null,
    val price: String? = null,
    val status: String,
    val addedBy: UserSummaryDto,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateBookRequest(
    val isbn: String? = null,
    val title: String,
    val author: String,
    val publisher: String? = null,
    val publicationYear: Int? = null,
    val edition: String? = null,
    val language: String = "English",
    val category: String,
    val subCategory: String? = null,
    val totalCopies: Int = 1,
    val shelfLocation: String? = null,
    val coverImageUrl: String? = null,
    val description: String? = null,
    val price: String? = null
)

@Serializable
data class UpdateBookRequest(
    val isbn: String? = null,
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val publicationYear: Int? = null,
    val edition: String? = null,
    val language: String? = null,
    val category: String? = null,
    val subCategory: String? = null,
    val totalCopies: Int? = null,
    val shelfLocation: String? = null,
    val coverImageUrl: String? = null,
    val description: String? = null,
    val price: String? = null,
    val status: String? = null
)

@Serializable
data class BookSearchRequest(
    val searchQuery: String? = null,  // Search title, author, ISBN
    val category: String? = null,
    val language: String? = null,
    val availableOnly: Boolean = false,
    val page: Int = 1,
    val pageSize: Int = 20
)

@Serializable
data class BookAvailabilityDto(
    val bookId: String,
    val title: String,
    val totalCopies: Int,
    val availableCopies: Int,
    val borrowedCopies: Int,
    val reservedCopies: Int,
    val isAvailable: Boolean,
    val nextAvailableDate: String? = null
)

// ============================================
// Borrowing DTOs
// ============================================

@Serializable
data class BorrowingDto(
    val id: String,
    val book: BookSummaryDto,
    val user: UserSummaryDto,
    val borrowedDate: String,
    val dueDate: String,
    val returnedDate: String? = null,
    val status: String,
    val renewalCount: Int,
    val issuedBy: UserSummaryDto,
    val returnedTo: UserSummaryDto? = null,
    val condition: String? = null,
    val notes: String? = null,
    val isOverdue: Boolean,
    val daysOverdue: Int? = null,
    val fine: FineDto? = null,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class BookSummaryDto(
    val id: String,
    val title: String,
    val author: String,
    val isbn: String? = null,
    val coverImageUrl: String? = null
)

@Serializable
data class IssueBorrow Request(
    val bookId: String,
    val userId: String,
    val dueDate: String? = null  // Optional, calculated from settings if not provided
)

@Serializable
data class ReturnBookRequest(
    val condition: String? = null,  // Good, Fair, Damaged
    val notes: String? = null
)

@Serializable
data class RenewBorrowingRequest(
    val reason: String? = null
)

// ============================================
// Reservation DTOs
// ============================================

@Serializable
data class ReservationDto(
    val id: String,
    val book: BookSummaryDto,
    val user: UserSummaryDto,
    val reservedDate: String,
    val expiryDate: String,
    val status: String,
    val notified: Boolean,
    val queuePosition: Int? = null,
    val estimatedAvailabilityDate: String? = null,
    val createdAt: String
)

@Serializable
data class CreateReservationRequest(
    val bookId: String
)

// ============================================
// Fine DTOs
// ============================================

@Serializable
data class FineDto(
    val id: String,
    val borrowing: BorrowingSummaryDto? = null,
    val user: UserSummaryDto,
    val fineType: String,
    val amount: String,
    val reason: String,
    val daysOverdue: Int? = null,
    val status: String,
    val paidAmount: String,
    val remainingAmount: String,
    val paidDate: String? = null,
    val paidTo: UserSummaryDto? = null,
    val waived: Boolean,
    val waivedBy: UserSummaryDto? = null,
    val waivedReason: String? = null,
    val createdAt: String
)

@Serializable
data class BorrowingSummaryDto(
    val id: String,
    val bookTitle: String,
    val borrowedDate: String,
    val dueDate: String
)

@Serializable
data class PayFineRequest(
    val amount: String,
    val paymentMethod: String? = null
)

@Serializable
data class WaiveFineRequest(
    val reason: String
)

@Serializable
data class UserFinesSummaryDto(
    val userId: String,
    val userName: String,
    val totalFines: String,
    val paidFines: String,
    val pendingFines: String,
    val fineCount: Int
)

// ============================================
// Library Settings DTOs
// ============================================

@Serializable
data class LibrarySettingsDto(
    val id: Int,
    val maxBooksPerStudent: Int,
    val maxBooksPerStaff: Int,
    val borrowingPeriodDays: Int,
    val maxRenewals: Int,
    val overdueFinePer Day: String,
    val lostBookFineMultiplier: String,
    val reservationExpiryDays: Int,
    val enableReservations: Boolean,
    val enableFines: Boolean,
    val updatedAt: String? = null,
    val updatedBy: UserSummaryDto? = null
)

@Serializable
data class UpdateLibrarySettingsRequest(
    val maxBooksPerStudent: Int? = null,
    val maxBooksPerStaff: Int? = null,
    val borrowingPeriodDays: Int? = null,
    val maxRenewals: Int? = null,
    val overdueFinePer Day: String? = null,
    val lostBookFineMultiplier: String? = null,
    val reservationExpiryDays: Int? = null,
    val enableReservations: Boolean? = null,
    val enableFines: Boolean? = null
)

// ============================================
// Dashboard & Stats DTOs
// ============================================

@Serializable
data class LibraryDashboardDto(
    val totalBooks: Int,
    val totalCopies: Int,
    val availableBooks: Int,
    val borrowedBooks: Int,
    val overdueBooks: Int,
    val activeReservations: Int,
    val totalFines: String,
    val pendingFines: String,
    val activeMembers: Int,
    val booksIssuedToday: Int,
    val booksReturnedToday: Int
)

@Serializable
data class PopularBookDto(
    val book: BookDto,
    val borrowCount: Int,
    val currentlyBorrowed: Int
)

@Serializable
data class UserBorrowingStatsDto(
    val user: UserSummaryDto,
    val totalBorrowings: Int,
    val activeBorrowings: Int,
    val overdueBorrowings: Int,
    val totalFines: String,
    val pendingFines: String,
    val canBorrow: Boolean,
    val reason: String? = null
)

@Serializable
data class BookCategoryStatsDto(
    val category: String,
    val totalBooks: Int,
    val availableBooks: Int,
    val borrowedBooks: Int,
    val popularityScore: Int
)
