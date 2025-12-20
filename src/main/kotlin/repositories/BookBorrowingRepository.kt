package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class BookBorrowingRepository {

    suspend fun create(
        bookId: UUID,
        userId: UUID,
        dueDate: LocalDateTime,
        issuedBy: UUID
    ): UUID = tenantDbQuery {
        val borrowingId = UUID.randomUUID()
        BookBorrowings.insert {
            it[id] = borrowingId
            it[BookBorrowings.bookId] = bookId
            it[BookBorrowings.userId] = userId
            it[borrowedDate] = LocalDateTime.now()
            it[BookBorrowings.dueDate] = dueDate
            it[status] = BorrowingStatus.ACTIVE
            it[BookBorrowings.issuedBy] = issuedBy
            it[createdAt] = LocalDateTime.now()
        }
        borrowingId
    }

    suspend fun findById(borrowingId: UUID): BorrowingDto? = tenantDbQuery {
        val bookAlias = Books.alias("book")
        val userAlias = Users.alias("user")
        val issuedByAlias = Users.alias("issued_by")

        BookBorrowings
            .join(bookAlias, JoinType.INNER, BookBorrowings.bookId, bookAlias[Books.id])
            .join(userAlias, JoinType.INNER, BookBorrowings.userId, userAlias[Users.id])
            .join(issuedByAlias, JoinType.INNER, BookBorrowings.issuedBy, issuedByAlias[Users.id])
            .selectAll()
            .where { BookBorrowings.id eq borrowingId }
            .map { mapRowToDto(it, bookAlias, userAlias, issuedByAlias, null) }
            .singleOrNull()
    }

    suspend fun findActiveByUserId(userId: UUID): List<BorrowingDto> = tenantDbQuery {
        val bookAlias = Books.alias("book")
        val userAlias = Users.alias("user")
        val issuedByAlias = Users.alias("issued_by")

        BookBorrowings
            .join(bookAlias, JoinType.INNER, BookBorrowings.bookId, bookAlias[Books.id])
            .join(userAlias, JoinType.INNER, BookBorrowings.userId, userAlias[Users.id])
            .join(issuedByAlias, JoinType.INNER, BookBorrowings.issuedBy, issuedByAlias[Users.id])
            .selectAll()
            .where {
                (BookBorrowings.userId eq userId) and
                (BookBorrowings.status eq BorrowingStatus.ACTIVE)
            }
            .orderBy(BookBorrowings.dueDate to SortOrder.ASC)
            .map { mapRowToDto(it, bookAlias, userAlias, issuedByAlias, null) }
    }

    suspend fun findOverdue(): List<BorrowingDto> = tenantDbQuery {
        val bookAlias = Books.alias("book")
        val userAlias = Users.alias("user")
        val issuedByAlias = Users.alias("issued_by")
        val now = LocalDateTime.now()

        BookBorrowings
            .join(bookAlias, JoinType.INNER, BookBorrowings.bookId, bookAlias[Books.id])
            .join(userAlias, JoinType.INNER, BookBorrowings.userId, userAlias[Users.id])
            .join(issuedByAlias, JoinType.INNER, BookBorrowings.issuedBy, issuedByAlias[Users.id])
            .selectAll()
            .where {
                (BookBorrowings.status eq BorrowingStatus.ACTIVE) and
                (BookBorrowings.dueDate less now)
            }
            .orderBy(BookBorrowings.dueDate to SortOrder.ASC)
            .map { mapRowToDto(it, bookAlias, userAlias, issuedByAlias, null) }
    }

    suspend fun returnBook(borrowingId: UUID, returnedTo: UUID, condition: String?, notes: String?): Boolean = tenantDbQuery {
        BookBorrowings.update({ BookBorrowings.id eq borrowingId }) {
            it[returnedDate] = LocalDateTime.now()
            it[status] = BorrowingStatus.RETURNED
            it[BookBorrowings.returnedTo] = returnedTo
            condition?.let { c -> it[BookBorrowings.condition] = c }
            notes?.let { n -> it[BookBorrowings.notes] = n }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun renewBorrowing(borrowingId: UUID, newDueDate: LocalDateTime): Boolean = tenantDbQuery {
        BookBorrowings.update({ BookBorrowings.id eq borrowingId }) {
            it[dueDate] = newDueDate
            with(SqlExpressionBuilder) {
                it[renewalCount] = renewalCount + 1
            }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun updateStatus(borrowingId: UUID, newStatus: BorrowingStatus): Boolean = tenantDbQuery {
        BookBorrowings.update({ BookBorrowings.id eq borrowingId }) {
            it[status] = newStatus
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun countActiveByUserId(userId: UUID): Int = tenantDbQuery {
        BookBorrowings
            .selectAll()
            .where {
                (BookBorrowings.userId eq userId) and
                (BookBorrowings.status eq BorrowingStatus.ACTIVE)
            }
            .count()
            .toInt()
    }

    suspend fun countActiveByBookId(bookId: UUID): Int = tenantDbQuery {
        BookBorrowings
            .selectAll()
            .where {
                (BookBorrowings.bookId eq bookId) and
                (BookBorrowings.status eq BorrowingStatus.ACTIVE)
            }
            .count()
            .toInt()
    }

    suspend fun hasActiveBorrowing(userId: UUID, bookId: UUID): Boolean = tenantDbQuery {
        BookBorrowings
            .selectAll()
            .where {
                (BookBorrowings.userId eq userId) and
                (BookBorrowings.bookId eq bookId) and
                (BookBorrowings.status eq BorrowingStatus.ACTIVE)
            }
            .count() > 0
    }

    suspend fun findAll(status: String?, page: Int, pageSize: Int): Pair<List<BorrowingDto>, Long> = tenantDbQuery {
        val bookAlias = Books.alias("book")
        val userAlias = Users.alias("user")
        val issuedByAlias = Users.alias("issued_by")

        var query = BookBorrowings
            .join(bookAlias, JoinType.INNER, BookBorrowings.bookId, bookAlias[Books.id])
            .join(userAlias, JoinType.INNER, BookBorrowings.userId, userAlias[Users.id])
            .join(issuedByAlias, JoinType.INNER, BookBorrowings.issuedBy, issuedByAlias[Users.id])
            .selectAll()

        status?.let {
            query = query.where { BookBorrowings.status eq BorrowingStatus.valueOf(it) }
        }

        val total = query.count()
        val offset = ((page - 1) * pageSize).toLong()

        val borrowings = query
            .limit(pageSize, offset)
            .orderBy(BookBorrowings.borrowedDate to SortOrder.DESC)
            .map { mapRowToDto(it, bookAlias, userAlias, issuedByAlias, null) }

        Pair(borrowings, total)
    }

    private fun mapRowToDto(
        row: ResultRow,
        bookAlias: Alias<Books>,
        userAlias: Alias<Users>,
        issuedByAlias: Alias<Users>,
        returnedToAlias: Alias<Users>?
    ): BorrowingDto {
        val dueDate = row[BookBorrowings.dueDate]
        val returnedDate = row[BookBorrowings.returnedDate]
        val now = LocalDateTime.now()
        val isOverdue = returnedDate == null && dueDate.isBefore(now)
        val daysOverdue = if (isOverdue) {
            java.time.Duration.between(dueDate, now).toDays().toInt()
        } else null

        return BorrowingDto(
            id = row[BookBorrowings.id].toString(),
            book = BookSummaryDto(
                id = row[bookAlias[Books.id]].toString(),
                title = row[bookAlias[Books.title]],
                author = row[bookAlias[Books.author]],
                isbn = row[bookAlias[Books.isbn]],
                coverImageUrl = row[bookAlias[Books.coverImageUrl]]
            ),
            user = UserSummaryDto(
                id = row[userAlias[Users.id]].toString(),
                firstName = row[userAlias[Users.firstName]],
                lastName = row[userAlias[Users.lastName]],
                email = row[userAlias[Users.email]],
                role = row[userAlias[Users.role]].name
            ),
            borrowedDate = row[BookBorrowings.borrowedDate].toString(),
            dueDate = row[BookBorrowings.dueDate].toString(),
            returnedDate = row[BookBorrowings.returnedDate]?.toString(),
            status = row[BookBorrowings.status].name,
            renewalCount = row[BookBorrowings.renewalCount],
            issuedBy = UserSummaryDto(
                id = row[issuedByAlias[Users.id]].toString(),
                firstName = row[issuedByAlias[Users.firstName]],
                lastName = row[issuedByAlias[Users.lastName]],
                email = row[issuedByAlias[Users.email]],
                role = row[issuedByAlias[Users.role]].name
            ),
            returnedTo = null, // TODO: Add when needed
            condition = row[BookBorrowings.condition],
            notes = row[BookBorrowings.notes],
            isOverdue = isOverdue,
            daysOverdue = daysOverdue,
            fine = null, // Will be populated by service layer
            createdAt = row[BookBorrowings.createdAt].toString(),
            updatedAt = row[BookBorrowings.updatedAt]?.toString()
        )
    }
}
