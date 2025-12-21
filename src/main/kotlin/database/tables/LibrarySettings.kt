package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

object LibrarySettings : Table("library_settings") {
    val id = integer("id").autoIncrement()
    val maxBooksPerStudent = integer("max_books_per_student").default(3)
    val maxBooksPerStaff = integer("max_books_per_staff").default(5)
    val borrowingPeriodDays = integer("borrowing_period_days").default(14)
    val maxRenewals = integer("max_renewals").default(2)
    val overdueFinePerDay = decimal("overdue_fine_per_day", 10, 2).default(BigDecimal("5.00"))
    val lostBookFineMultiplier = decimal("lost_book_fine_multiplier", 5, 2).default(BigDecimal("2.00"))
    val reservationExpiryDays = integer("reservation_expiry_days").default(3)
    val enableReservations = bool("enable_reservations").default(true)
    val enableFines = bool("enable_fines").default(true)
    val updatedAt = datetime("updated_at").nullable()
    val updatedBy = uuid("updated_by").references(Users.id).nullable()

    override val primaryKey = PrimaryKey(id)
}
