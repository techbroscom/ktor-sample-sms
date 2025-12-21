package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class LibraryFineRepository {

    suspend fun create(
        borrowingId: UUID,
        userId: UUID,
        fineType: FineType,
        amount: BigDecimal,
        reason: String,
        daysOverdue: Int?
    ): UUID = tenantDbQuery {
        val fineId = UUID.randomUUID()
        LibraryFines.insert {
            it[id] = fineId
            it[LibraryFines.borrowingId] = borrowingId
            it[LibraryFines.userId] = userId
            it[LibraryFines.fineType] = fineType
            it[LibraryFines.amount] = amount
            it[LibraryFines.reason] = reason
            it[LibraryFines.daysOverdue] = daysOverdue
            it[status] = FineStatus.PENDING
            it[createdAt] = LocalDateTime.now()
        }
        fineId
    }

    suspend fun findById(fineId: UUID): FineDto? = tenantDbQuery {
        val userAlias = Users.alias("user")
        val borrowingAlias = BookBorrowings.alias("borrowing")
        val bookAlias = Books.alias("book")

        LibraryFines
            .join(userAlias, JoinType.INNER, LibraryFines.userId, userAlias[Users.id])
            .join(borrowingAlias, JoinType.LEFT, LibraryFines.borrowingId, borrowingAlias[BookBorrowings.id])
            .join(bookAlias, JoinType.LEFT, borrowingAlias[BookBorrowings.bookId], bookAlias[Books.id])
            .selectAll()
            .where { LibraryFines.id eq fineId }
            .map { mapRowToDto(it, userAlias, borrowingAlias, bookAlias) }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: UUID, status: String?): List<FineDto> = tenantDbQuery {
        val userAlias = Users.alias("user")
        val borrowingAlias = BookBorrowings.alias("borrowing")
        val bookAlias = Books.alias("book")

        var query = LibraryFines
            .join(userAlias, JoinType.INNER, LibraryFines.userId, userAlias[Users.id])
            .join(borrowingAlias, JoinType.LEFT, LibraryFines.borrowingId, borrowingAlias[BookBorrowings.id])
            .join(bookAlias, JoinType.LEFT, borrowingAlias[BookBorrowings.bookId], bookAlias[Books.id])
            .selectAll()
            .where { LibraryFines.userId eq userId }

        status?.let {
            query = query.andWhere { LibraryFines.status eq FineStatus.valueOf(it) }
        }

        query
            .orderBy(LibraryFines.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it, userAlias, borrowingAlias, bookAlias) }
    }

    suspend fun payFine(fineId: UUID, paidAmount: BigDecimal, paidTo: UUID): Boolean = tenantDbQuery {
        val fine = LibraryFines.selectAll().where { LibraryFines.id eq fineId }.singleOrNull()
            ?: return@tenantDbQuery false

        val currentPaid = fine[LibraryFines.paidAmount]
        val totalAmount = fine[LibraryFines.amount]
        val newPaidAmount = currentPaid + paidAmount

        val newStatus = when {
            newPaidAmount >= totalAmount -> FineStatus.PAID
            newPaidAmount > BigDecimal.ZERO -> FineStatus.PARTIALLY_PAID
            else -> FineStatus.PENDING
        }

        LibraryFines.update({ LibraryFines.id eq fineId }) {
            with(SqlExpressionBuilder) {
                it[LibraryFines.paidAmount] = LibraryFines.paidAmount + paidAmount
            }
            it[status] = newStatus
            it[paidDate] = LocalDateTime.now()
            it[LibraryFines.paidTo] = paidTo
        } > 0
    }

    suspend fun waiveFine(fineId: UUID, waivedBy: UUID, reason: String): Boolean = tenantDbQuery {
        LibraryFines.update({ LibraryFines.id eq fineId }) {
            it[waived] = true
            it[status] = FineStatus.WAIVED
            it[LibraryFines.waivedBy] = waivedBy
            it[waivedReason] = reason
        } > 0
    }

    suspend fun getTotalPendingByUserId(userId: UUID): BigDecimal = tenantDbQuery {
        LibraryFines
            .select(LibraryFines.amount, LibraryFines.paidAmount)
            .where {
                (LibraryFines.userId eq userId) and
                (LibraryFines.status inList listOf(FineStatus.PENDING, FineStatus.PARTIALLY_PAID))
            }
            .sumOf { it[LibraryFines.amount] - it[LibraryFines.paidAmount] }
    }

    suspend fun findAll(status: String?, page: Int, pageSize: Int): Pair<List<FineDto>, Long> = tenantDbQuery {
        val userAlias = Users.alias("user")
        val borrowingAlias = BookBorrowings.alias("borrowing")
        val bookAlias = Books.alias("book")

        var query = LibraryFines
            .join(userAlias, JoinType.INNER, LibraryFines.userId, userAlias[Users.id])
            .join(borrowingAlias, JoinType.LEFT, LibraryFines.borrowingId, borrowingAlias[BookBorrowings.id])
            .join(bookAlias, JoinType.LEFT, borrowingAlias[BookBorrowings.bookId], bookAlias[Books.id])
            .selectAll()

        status?.let {
            query = query.where { LibraryFines.status eq FineStatus.valueOf(it) }
        }

        val total = query.count()
        val offset = ((page - 1) * pageSize).toLong()

        val fines = query
            .limit(pageSize).offset(offset)
            .orderBy(LibraryFines.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it, userAlias, borrowingAlias, bookAlias) }

        Pair(fines, total)
    }

    private fun mapRowToDto(
        row: ResultRow,
        userAlias: Alias<Users>,
        borrowingAlias: Alias<BookBorrowings>,
        bookAlias: Alias<Books>
    ): FineDto {
        val amount = row[LibraryFines.amount]
        val paidAmount = row[LibraryFines.paidAmount]

        return FineDto(
            id = row[LibraryFines.id].toString(),
            borrowing = row.getOrNull(borrowingAlias[BookBorrowings.id])?.let {
                BorrowingSummaryDto(
                    id = it.toString(),
                    bookTitle = row[bookAlias[Books.title]],
                    borrowedDate = row[borrowingAlias[BookBorrowings.borrowedDate]].toString(),
                    dueDate = row[borrowingAlias[BookBorrowings.dueDate]].toString()
                )
            },
            user = UserSummaryDto(
                id = row[userAlias[Users.id]].toString(),
                firstName = row[userAlias[Users.firstName]],
                lastName = row[userAlias[Users.lastName]],
                email = row[userAlias[Users.email]],
                role = row[userAlias[Users.role]].name
            ),
            fineType = row[LibraryFines.fineType].name,
            amount = amount.toString(),
            reason = row[LibraryFines.reason],
            daysOverdue = row[LibraryFines.daysOverdue],
            status = row[LibraryFines.status].name,
            paidAmount = paidAmount.toString(),
            remainingAmount = (amount - paidAmount).toString(),
            paidDate = row[LibraryFines.paidDate]?.toString(),
            paidTo = null, // TODO: Add when needed
            waived = row[LibraryFines.waived],
            waivedBy = null, // TODO: Add when needed
            waivedReason = row[LibraryFines.waivedReason],
            createdAt = row[LibraryFines.createdAt].toString()
        )
    }
}
