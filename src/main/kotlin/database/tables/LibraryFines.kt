package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal
import java.time.LocalDateTime

enum class FineType {
    OVERDUE,     // Late return
    LOST_BOOK,   // Book declared lost
    DAMAGE       // Book returned damaged
}

enum class FineStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    WAIVED
}

object LibraryFines : Table("library_fines") {
    val id = uuid("id")
    val borrowingId = uuid("borrowing_id").references(BookBorrowings.id)
    val userId = uuid("user_id").references(Users.id)
    val fineType = enumerationByName("fine_type", 20, FineType::class)
    val amount = decimal("amount", 10, 2)
    val reason = varchar("reason", 500)
    val daysOverdue = integer("days_overdue").nullable()
    val status = enumerationByName("status", 20, FineStatus::class)
    val paidAmount = decimal("paid_amount", 10, 2).default(BigDecimal.ZERO)
    val paidDate = datetime("paid_date").nullable()
    val paidTo = uuid("paid_to").references(Users.id).nullable()
    val waived = bool("waived").default(false)
    val waivedBy = uuid("waived_by").references(Users.id).nullable()
    val waivedReason = varchar("waived_reason", 500).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, userId)
        index(false, status)
        index(false, borrowingId)
    }
}
