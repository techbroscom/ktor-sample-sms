package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class BorrowingStatus {
    ACTIVE,      // Currently borrowed
    RETURNED,    // Returned on time
    OVERDUE,     // Past due date, not returned
    LOST,        // Book declared lost
    DAMAGED      // Book returned damaged
}

object BookBorrowings : Table("book_borrowings") {
    val id = uuid("id")
    val bookId = uuid("book_id").references(Books.id)
    val userId = uuid("user_id").references(Users.id)
    val borrowedDate = datetime("borrowed_date").default(LocalDateTime.now())
    val dueDate = datetime("due_date")
    val returnedDate = datetime("returned_date").nullable()
    val status = enumerationByName("status", 20, BorrowingStatus::class)
    val renewalCount = integer("renewal_count").default(0)
    val issuedBy = uuid("issued_by").references(Users.id)
    val returnedTo = uuid("returned_to").references(Users.id).nullable()
    val condition = varchar("condition", 50).nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, bookId)
        index(false, userId)
        index(false, status)
        index(false, dueDate)
    }
}
