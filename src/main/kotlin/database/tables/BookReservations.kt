package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class ReservationStatus {
    PENDING,     // Waiting for book to be available
    AVAILABLE,   // Book is ready for pickup
    FULFILLED,   // User picked up the book
    EXPIRED,     // Reservation expired
    CANCELLED    // User cancelled
}

object BookReservations : Table("book_reservations") {
    val id = uuid("id")
    val bookId = uuid("book_id").references(Books.id)
    val userId = uuid("user_id").references(Users.id)
    val reservedDate = datetime("reserved_date").default(LocalDateTime.now())
    val expiryDate = datetime("expiry_date")
    val status = enumerationByName("status", 20, ReservationStatus::class)
    val notified = bool("notified").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, bookId)
        index(false, userId)
        index(false, status)
    }
}
