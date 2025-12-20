package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.LibraryService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.libraryRoutes(libraryService: LibraryService) {

    // ============================================
    // Book Management Routes
    // ============================================

    route("/api/v1/library/books") {

        // Create new book
        post {
            val request = call.receive<CreateBookRequest>()
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val book = libraryService.createBook(request, userId)
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    data = book,
                    message = "Book added successfully"
                )
            )
        }

        // Search/List books
        get {
            val searchRequest = BookSearchRequest(
                searchQuery = call.request.queryParameters["search"],
                category = call.request.queryParameters["category"],
                language = call.request.queryParameters["language"],
                availableOnly = call.request.queryParameters["availableOnly"]?.toBoolean() ?: false,
                page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1,
                pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            )

            val result = libraryService.searchBooks(searchRequest)
            call.respond(result)
        }

        // Get book by ID
        get("/{id}") {
            val bookId = call.parameters["id"]
                ?: throw ApiException("Book ID is required", HttpStatusCode.BadRequest)

            val book = libraryService.getBook(bookId)
            call.respond(ApiResponse(success = true, data = book))
        }

        // Update book
        put("/{id}") {
            val bookId = call.parameters["id"]
                ?: throw ApiException("Book ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateBookRequest>()
            val book = libraryService.updateBook(bookId, request)
            call.respond(ApiResponse(
                success = true,
                data = book,
                message = "Book updated successfully"
            ))
        }

        // Delete book
        delete("/{id}") {
            val bookId = call.parameters["id"]
                ?: throw ApiException("Book ID is required", HttpStatusCode.BadRequest)

            libraryService.deleteBook(bookId)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Unit>(
                    success = true,
                    data = null,
                    message = "Book deleted successfully"
                )
            )
        }

        // Get all categories
        get("/categories") {
            val categories = libraryService.getBookCategories()
            call.respond(ApiResponse(success = true, data = categories))
        }
    }

    // ============================================
    // Borrowing Management Routes
    // ============================================

    route("/api/v1/library/borrowings") {

        // Issue book to user
        post {
            val request = call.receive<IssueBorrowRequest>()
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val borrowing = libraryService.issueBook(request, userId)
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    data = borrowing,
                    message = "Book issued successfully"
                )
            )
        }

        // Get all borrowings with optional status filter
        get {
            val status = call.request.queryParameters["status"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val result = libraryService.getAllBorrowings(status, page, pageSize)
            call.respond(result)
        }

        // Get active borrowings
        get("/active") {
            val borrowings = libraryService.getActiveBorrowings()
            call.respond(ApiResponse(
                success = true,
                data = borrowings,
                message = "Active borrowings"
            ))
        }

        // Get overdue borrowings
        get("/overdue") {
            val borrowings = libraryService.getOverdueBorrowings()
            call.respond(ApiResponse(
                success = true,
                data = borrowings,
                message = "Overdue borrowings"
            ))
        }

        // Get user's borrowing history
        get("/user/{userId}") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val borrowings = libraryService.getUserBorrowingHistory(userId)
            call.respond(ApiResponse(success = true, data = borrowings))
        }

        // Get borrowing by ID
        get("/{id}") {
            val borrowingId = call.parameters["id"]
                ?: throw ApiException("Borrowing ID is required", HttpStatusCode.BadRequest)

            val borrowing = libraryService.getBorrowing(borrowingId)
            call.respond(ApiResponse(success = true, data = borrowing))
        }

        // Return book
        post("/{id}/return") {
            val borrowingId = call.parameters["id"]
                ?: throw ApiException("Borrowing ID is required", HttpStatusCode.BadRequest)

            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val request = call.receiveNullable<ReturnBookRequest>() ?: ReturnBookRequest()
            val borrowing = libraryService.returnBook(borrowingId, userId, request)
            call.respond(ApiResponse(
                success = true,
                data = borrowing,
                message = "Book returned successfully"
            ))
        }

        // Renew borrowing
        post("/{id}/renew") {
            val borrowingId = call.parameters["id"]
                ?: throw ApiException("Borrowing ID is required", HttpStatusCode.BadRequest)

            val borrowing = libraryService.renewBorrowing(borrowingId)
            call.respond(ApiResponse(
                success = true,
                data = borrowing,
                message = "Borrowing renewed successfully"
            ))
        }
    }

    // ============================================
    // Reservation Management Routes
    // ============================================

    route("/api/v1/library/reservations") {

        // Create reservation
        post {
            val request = call.receive<CreateReservationRequest>()
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val reservation = libraryService.createReservation(request, userId)
            call.respond(
                HttpStatusCode.Created,
                ApiResponse(
                    success = true,
                    data = reservation,
                    message = "Book reserved successfully"
                )
            )
        }

        // Get user's reservations
        get("/my-reservations") {
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val status = call.request.queryParameters["status"]
            val reservations = libraryService.getUserReservations(userId, status)
            call.respond(ApiResponse(success = true, data = reservations))
        }

        // Get reservation by ID
        get("/{id}") {
            val reservationId = call.parameters["id"]
                ?: throw ApiException("Reservation ID is required", HttpStatusCode.BadRequest)

            val reservation = libraryService.getReservation(reservationId)
            call.respond(ApiResponse(success = true, data = reservation))
        }

        // Cancel reservation
        delete("/{id}") {
            val reservationId = call.parameters["id"]
                ?: throw ApiException("Reservation ID is required", HttpStatusCode.BadRequest)

            val reservation = libraryService.cancelReservation(reservationId)
            call.respond(ApiResponse(
                success = true,
                data = reservation,
                message = "Reservation cancelled successfully"
            ))
        }
    }

    // ============================================
    // Fine Management Routes
    // ============================================

    route("/api/v1/library/fines") {

        // Get user's fines
        get("/user/{userId}") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val status = call.request.queryParameters["status"]
            val fines = libraryService.getUserFines(userId, status)
            call.respond(ApiResponse(success = true, data = fines))
        }

        // Pay fine
        post("/{id}/pay") {
            val fineId = call.parameters["id"]
                ?: throw ApiException("Fine ID is required", HttpStatusCode.BadRequest)

            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val request = call.receive<PayFineRequest>()
            val fine = libraryService.payFine(fineId, request, userId)
            call.respond(ApiResponse(
                success = true,
                data = fine,
                message = "Fine payment recorded successfully"
            ))
        }

        // Waive fine
        post("/{id}/waive") {
            val fineId = call.parameters["id"]
                ?: throw ApiException("Fine ID is required", HttpStatusCode.BadRequest)

            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val request = call.receive<WaiveFineRequest>()
            val fine = libraryService.waiveFine(fineId, request, userId)
            call.respond(ApiResponse(
                success = true,
                data = fine,
                message = "Fine waived successfully"
            ))
        }
    }

    // ============================================
    // Library Settings Routes
    // ============================================

    route("/api/v1/library/settings") {

        // Get library settings
        get {
            val settings = libraryService.getLibrarySettings()
            call.respond(ApiResponse(success = true, data = settings))
        }

        // Update library settings
        put {
            val request = call.receive<UpdateLibrarySettingsRequest>()
            val userId = call.request.header("X-User-Id")
                ?: throw ApiException("User ID header is required", HttpStatusCode.Unauthorized)

            val settings = libraryService.updateLibrarySettings(request, userId)
            call.respond(ApiResponse(
                success = true,
                data = settings,
                message = "Library settings updated successfully"
            ))
        }
    }
}
