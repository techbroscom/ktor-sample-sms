package com.example.services

import com.example.database.tables.*
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.PaginatedResponse
import com.example.repositories.*
import com.example.tenant.TenantContextHolder
import io.ktor.http.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class LibraryService(
    private val bookRepository: BookRepository,
    private val borrowingRepository: BookBorrowingRepository,
    private val reservationRepository: BookReservationRepository,
    private val fineRepository: LibraryFineRepository,
    private val settingsRepository: LibrarySettingsRepository,
    private val userRepository: UserRepository
) {

    // ============================================
    // Book Management
    // ============================================

    suspend fun createBook(request: CreateBookRequest, addedBy: String): BookDto {
        validateCreateBookRequest(request)

        // Check for duplicate ISBN
        request.isbn?.let { isbn ->
            if (bookRepository.findByIsbn(isbn) != null) {
                throw ApiException("Book with ISBN $isbn already exists", HttpStatusCode.Conflict)
            }
        }

        val addedByUUID = UUID.fromString(addedBy)
        val bookId = bookRepository.create(request, addedByUUID)

        return bookRepository.findById(bookId)
            ?: throw ApiException("Failed to retrieve created book", HttpStatusCode.InternalServerError)
    }

    suspend fun getBook(bookId: String): BookDto {
        val id = UUID.fromString(bookId)
        return bookRepository.findById(id)
            ?: throw ApiException("Book not found", HttpStatusCode.NotFound)
    }

    suspend fun searchBooks(request: BookSearchRequest): PaginatedResponse<List<BookDto>> {
        val (books, total) = bookRepository.search(request)
        val totalPages = ((total + request.pageSize - 1) / request.pageSize)

        return PaginatedResponse(
            success = true,
            data = books,
            pagination = PaginatedResponse.PaginationInfo(
                page = request.page,
                pageSize = request.pageSize,
                totalItems = total,
                totalPages = totalPages
            )
        )
    }

    suspend fun updateBook(bookId: String, request: UpdateBookRequest): BookDto {
        val id = UUID.fromString(bookId)

        // Verify book exists
        bookRepository.findById(id)
            ?: throw ApiException("Book not found", HttpStatusCode.NotFound)

        val success = bookRepository.update(id, request)
        if (!success) {
            throw ApiException("Failed to update book", HttpStatusCode.InternalServerError)
        }

        return bookRepository.findById(id)!!
    }

    suspend fun deleteBook(bookId: String) {
        val id = UUID.fromString(bookId)

        // Check if book has active borrowings
        val activeBorrowings = borrowingRepository.countActiveByBookId(id)
        if (activeBorrowings > 0) {
            throw ApiException(
                "Cannot delete book with active borrowings. Return all copies first.",
                HttpStatusCode.BadRequest
            )
        }

        val success = bookRepository.delete(id)
        if (!success) {
            throw ApiException("Book not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getBookCategories(): List<String> {
        return bookRepository.getAllCategories()
    }

    // ============================================
    // Borrowing Management
    // ============================================

    suspend fun issueBook(request: IssueBorrowRequest, issuedBy: String): BorrowingDto {
        val bookId = UUID.fromString(request.bookId)
        val userId = UUID.fromString(request.userId)
        val issuedByUUID = UUID.fromString(issuedBy)

        // Get library settings
        val settings = settingsRepository.getOrCreateSettings()

        // Verify book exists and is available
        val book = bookRepository.findById(bookId)
            ?: throw ApiException("Book not found", HttpStatusCode.NotFound)

        if (book.availableCopies <= 0) {
            throw ApiException("No copies available for borrowing", HttpStatusCode.BadRequest)
        }

        // Verify user exists
        val user = userRepository.findById(userId)
            ?: throw ApiException("User not found", HttpStatusCode.NotFound)

        // Check borrowing limit
        val activeBorrowings = borrowingRepository.countActiveByUserId(userId)
        val maxBooks = if (user.role == "STUDENT") settings.maxBooksPerStudent else settings.maxBooksPerStaff

        if (activeBorrowings >= maxBooks) {
            throw ApiException(
                "User has reached maximum borrowing limit of $maxBooks books",
                HttpStatusCode.BadRequest
            )
        }

        // Check if user already has this book borrowed
        if (borrowingRepository.hasActiveBorrowing(userId, bookId)) {
            throw ApiException("User already has this book borrowed", HttpStatusCode.BadRequest)
        }

        // Check for pending fines
        if (settings.enableFines) {
            val pendingFines = fineRepository.getTotalPendingByUserId(userId)
            if (pendingFines > BigDecimal.ZERO) {
                throw ApiException(
                    "Cannot borrow books with pending fines of ₹$pendingFines. Please pay fines first.",
                    HttpStatusCode.BadRequest
                )
            }
        }

        // Calculate due date
        val dueDate = request.dueDate?.let { LocalDateTime.parse(it) }
            ?: LocalDateTime.now().plusDays(settings.borrowingPeriodDays.toLong())

        // Create borrowing
        val borrowingId = borrowingRepository.create(bookId, userId, dueDate, issuedByUUID)

        // Update book availability
        bookRepository.updateAvailability(bookId, -1)

        return borrowingRepository.findById(borrowingId)
            ?: throw ApiException("Failed to retrieve created borrowing", HttpStatusCode.InternalServerError)
    }

    suspend fun returnBook(borrowingId: String, returnedTo: String, request: ReturnBookRequest): BorrowingDto {
        val id = UUID.fromString(borrowingId)
        val returnedToUUID = UUID.fromString(returnedTo)

        // Get borrowing
        val borrowing = borrowingRepository.findById(id)
            ?: throw ApiException("Borrowing not found", HttpStatusCode.NotFound)

        if (borrowing.status != BorrowingStatus.ACTIVE.name) {
            throw ApiException(
                "Only active borrowings can be returned. Current status: ${borrowing.status}",
                HttpStatusCode.BadRequest
            )
        }

        // Return book
        val success = borrowingRepository.returnBook(id, returnedToUUID, request.condition, request.notes)
        if (!success) {
            throw ApiException("Failed to return book", HttpStatusCode.InternalServerError)
        }

        // Update book availability
        val bookId = UUID.fromString(borrowing.book.id)
        bookRepository.updateAvailability(bookId, 1)

        // Handle fines if overdue
        if (borrowing.isOverdue) {
            val settings = settingsRepository.getOrCreateSettings()
            if (settings.enableFines) {
                createOverdueFine(id, UUID.fromString(borrowing.user.id), borrowing.daysOverdue!!, settings)
            }
        }

        // Handle damaged book fine
        if (request.condition == "Damaged") {
            // Create damage fine (could be customized)
            // For now, we'll skip this and let admin manually add damage fines
        }

        // Notify first person in reservation queue
        val pendingReservations = reservationRepository.findPendingByBookId(bookId)
        if (pendingReservations.isNotEmpty()) {
            val firstReservation = pendingReservations.first()
            reservationRepository.updateStatus(UUID.fromString(firstReservation.id), ReservationStatus.AVAILABLE)
            reservationRepository.markNotified(UUID.fromString(firstReservation.id))
            // TODO: Send notification to user
        }

        return borrowingRepository.findById(id)!!
    }

    suspend fun renewBorrowing(borrowingId: String): BorrowingDto {
        val id = UUID.fromString(borrowingId)

        // Get borrowing
        val borrowing = borrowingRepository.findById(id)
            ?: throw ApiException("Borrowing not found", HttpStatusCode.NotFound)

        if (borrowing.status != BorrowingStatus.ACTIVE.name) {
            throw ApiException("Only active borrowings can be renewed", HttpStatusCode.BadRequest)
        }

        // Get settings
        val settings = settingsRepository.getOrCreateSettings()

        // Check renewal limit
        if (borrowing.renewalCount >= settings.maxRenewals) {
            throw ApiException(
                "Maximum renewals (${ settings.maxRenewals}) reached for this borrowing",
                HttpStatusCode.BadRequest
            )
        }

        // Check if book has pending reservations
        val bookId = UUID.fromString(borrowing.book.id)
        val pendingReservations = reservationRepository.countPendingByBookId(bookId)
        if (pendingReservations > 0) {
            throw ApiException(
                "Cannot renew: book has pending reservations",
                HttpStatusCode.BadRequest
            )
        }

        // Calculate new due date
        val newDueDate = LocalDateTime.parse(borrowing.dueDate).plusDays(settings.borrowingPeriodDays.toLong())

        // Renew
        val success = borrowingRepository.renewBorrowing(id, newDueDate)
        if (!success) {
            throw ApiException("Failed to renew borrowing", HttpStatusCode.InternalServerError)
        }

        return borrowingRepository.findById(id)!!
    }

    suspend fun getBorrowing(borrowingId: String): BorrowingDto {
        val id = UUID.fromString(borrowingId)
        return borrowingRepository.findById(id)
            ?: throw ApiException("Borrowing not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllBorrowings(status: String?, page: Int, pageSize: Int): PaginatedResponse<List<BorrowingDto>> {
        val (borrowings, total) = borrowingRepository.findAll(status, page, pageSize)
        val totalPages = ((total + pageSize - 1) / pageSize)

        return PaginatedResponse(
            success = true,
            data = borrowings,
            pagination = PaginatedResponse.PaginationInfo(
                page = page,
                pageSize = pageSize,
                totalItems = total,
                totalPages = totalPages
            )
        )
    }

    suspend fun getActiveBorrowings(): List<BorrowingDto> {
        val (borrowings, _) = borrowingRepository.findAll("ACTIVE", 1, 1000)
        return borrowings
    }

    suspend fun getOverdueBorrowings(): List<BorrowingDto> {
        return borrowingRepository.findOverdue()
    }

    suspend fun getUserBorrowingHistory(userId: String): List<BorrowingDto> {
        val id = UUID.fromString(userId)
        val (borrowings, _) = borrowingRepository.findAll(null, 1, 100) // TODO: Filter by user
        return borrowings
    }

    // ============================================
    // Reservation Management
    // ============================================

    suspend fun createReservation(request: CreateReservationRequest, userId: String): ReservationDto {
        val bookId = UUID.fromString(request.bookId)
        val userUUID = UUID.fromString(userId)

        // Check if reservations are enabled
        val settings = settingsRepository.getOrCreateSettings()
        if (!settings.enableReservations) {
            throw ApiException("Reservations are not enabled", HttpStatusCode.BadRequest)
        }

        // Verify book exists
        val book = bookRepository.findById(bookId)
            ?: throw ApiException("Book not found", HttpStatusCode.NotFound)

        // Check if user already has an active reservation for this book
        if (reservationRepository.hasActiveReservation(userUUID, bookId)) {
            throw ApiException("You already have an active reservation for this book", HttpStatusCode.BadRequest)
        }

        // Check if user currently has this book borrowed
        if (borrowingRepository.hasActiveBorrowing(userUUID, bookId)) {
            throw ApiException("You already have this book borrowed", HttpStatusCode.BadRequest)
        }

        // Calculate expiry date
        val expiryDate = LocalDateTime.now().plusDays(settings.reservationExpiryDays.toLong())

        // Create reservation
        val reservationId = reservationRepository.create(bookId, userUUID, expiryDate)

        return reservationRepository.findById(reservationId)
            ?: throw ApiException("Failed to retrieve created reservation", HttpStatusCode.InternalServerError)
    }

    suspend fun getReservation(reservationId: String): ReservationDto {
        val id = UUID.fromString(reservationId)
        return reservationRepository.findById(id)
            ?: throw ApiException("Reservation not found", HttpStatusCode.NotFound)
    }

    suspend fun getUserReservations(userId: String, status: String?): List<ReservationDto> {
        val id = UUID.fromString(userId)
        return reservationRepository.findByUserId(id, status)
    }

    suspend fun cancelReservation(reservationId: String): ReservationDto {
        val id = UUID.fromString(reservationId)

        val reservation = reservationRepository.findById(id)
            ?: throw ApiException("Reservation not found", HttpStatusCode.NotFound)

        if (reservation.status == ReservationStatus.FULFILLED.name) {
            throw ApiException("Cannot cancel fulfilled reservations", HttpStatusCode.BadRequest)
        }

        reservationRepository.updateStatus(id, ReservationStatus.CANCELLED)

        return reservationRepository.findById(id)!!
    }

    // ============================================
    // Fine Management
    // ============================================

    suspend fun getUserFines(userId: String, status: String?): List<FineDto> {
        val id = UUID.fromString(userId)
        return fineRepository.findByUserId(id, status)
    }

    suspend fun payFine(fineId: String, request: PayFineRequest, paidTo: String): FineDto {
        val id = UUID.fromString(fineId)
        val paidToUUID = UUID.fromString(paidTo)

        val fine = fineRepository.findById(id)
            ?: throw ApiException("Fine not found", HttpStatusCode.NotFound)

        val amount = BigDecimal(request.amount)
        val remaining = BigDecimal(fine.remainingAmount)

        if (amount > remaining) {
            throw ApiException("Payment amount exceeds remaining fine amount", HttpStatusCode.BadRequest)
        }

        val success = fineRepository.payFine(id, amount, paidToUUID)
        if (!success) {
            throw ApiException("Failed to process payment", HttpStatusCode.InternalServerError)
        }

        return fineRepository.findById(id)!!
    }

    suspend fun waiveFine(fineId: String, request: WaiveFineRequest, waivedBy: String): FineDto {
        val id = UUID.fromString(fineId)
        val waivedByUUID = UUID.fromString(waivedBy)

        fineRepository.findById(id)
            ?: throw ApiException("Fine not found", HttpStatusCode.NotFound)

        val success = fineRepository.waiveFine(id, waivedByUUID, request.reason)
        if (!success) {
            throw ApiException("Failed to waive fine", HttpStatusCode.InternalServerError)
        }

        return fineRepository.findById(id)!!
    }

    // ============================================
    // Settings Management
    // ============================================

    suspend fun getLibrarySettings(): LibrarySettingsDto {
        return settingsRepository.getOrCreateSettings()
    }

    suspend fun updateLibrarySettings(request: UpdateLibrarySettingsRequest, updatedBy: String): LibrarySettingsDto {
        val updatedByUUID = UUID.fromString(updatedBy)

        val success = settingsRepository.updateSettings(request, updatedByUUID)
        if (!success) {
            throw ApiException("Failed to update settings", HttpStatusCode.InternalServerError)
        }

        return settingsRepository.getOrCreateSettings()
    }

    // ============================================
    // Helper Methods
    // ============================================

    private suspend fun createOverdueFine(
        borrowingId: UUID,
        userId: UUID,
        daysOverdue: Int,
        settings: LibrarySettingsDto
    ) {
        val finePerDay = BigDecimal(settings.overdueFinePer Day)
        val totalFine = finePerDay.multiply(BigDecimal(daysOverdue))

        fineRepository.create(
            borrowingId = borrowingId,
            userId = userId,
            fineType = FineType.OVERDUE,
            amount = totalFine,
            reason = "Overdue return: $daysOverdue days late",
            daysOverdue = daysOverdue
        )
    }

    private fun validateCreateBookRequest(request: CreateBookRequest) {
        if (request.title.isBlank()) {
            throw ApiException("Title is required", HttpStatusCode.BadRequest)
        }
        if (request.author.isBlank()) {
            throw ApiException("Author is required", HttpStatusCode.BadRequest)
        }
        if (request.category.isBlank()) {
            throw ApiException("Category is required", HttpStatusCode.BadRequest)
        }
        if (request.totalCopies < 1) {
            throw ApiException("Total copies must be at least 1", HttpStatusCode.BadRequest)
        }
    }
}
