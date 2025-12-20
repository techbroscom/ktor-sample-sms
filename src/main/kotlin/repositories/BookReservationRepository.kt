package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class BookReservationRepository {

    suspend fun create(bookId: UUID, userId: UUID, expiryDate: LocalDateTime): UUID = tenantDbQuery {
        val reservationId = UUID.randomUUID()
        BookReservations.insert {
            it[id] = reservationId
            it[BookReservations.bookId] = bookId
            it[BookReservations.userId] = userId
            it[reservedDate] = LocalDateTime.now()
            it[BookReservations.expiryDate] = expiryDate
            it[status] = ReservationStatus.PENDING
            it[createdAt] = LocalDateTime.now()
        }
        reservationId
    }

    suspend fun findById(reservationId: UUID): ReservationDto? = tenantDbQuery {
        val bookAlias = Books.alias("book")
        val userAlias = Users.alias("user")

        BookReservations
            .join(bookAlias, JoinType.INNER, BookReservations.bookId, bookAlias[Books.id])
            .join(userAlias, JoinType.INNER, BookReservations.userId, userAlias[Users.id])
            .selectAll()
            .where { BookReservations.id eq reservationId }
            .map { mapRowToDto(it, bookAlias, userAlias) }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: UUID, status: String?): List<ReservationDto> = tenantDbQuery {
        val bookAlias = Books.alias("book")
        val userAlias = Users.alias("user")

        var query = BookReservations
            .join(bookAlias, JoinType.INNER, BookReservations.bookId, bookAlias[Books.id])
            .join(userAlias, JoinType.INNER, BookReservations.userId, userAlias[Users.id])
            .selectAll()
            .where { BookReservations.userId eq userId }

        status?.let {
            query = query.andWhere { BookReservations.status eq ReservationStatus.valueOf(it) }
        }

        query
            .orderBy(BookReservations.reservedDate to SortOrder.DESC)
            .map { mapRowToDto(it, bookAlias, userAlias) }
    }

    suspend fun findPendingByBookId(bookId: UUID): List<ReservationDto> = tenantDbQuery {
        val bookAlias = Books.alias("book")
        val userAlias = Users.alias("user")

        BookReservations
            .join(bookAlias, JoinType.INNER, BookReservations.bookId, bookAlias[Books.id])
            .join(userAlias, JoinType.INNER, BookReservations.userId, userAlias[Users.id])
            .selectAll()
            .where {
                (BookReservations.bookId eq bookId) and
                (BookReservations.status eq ReservationStatus.PENDING)
            }
            .orderBy(BookReservations.reservedDate to SortOrder.ASC)
            .map { mapRowToDto(it, bookAlias, userAlias) }
    }

    suspend fun updateStatus(reservationId: UUID, newStatus: ReservationStatus): Boolean = tenantDbQuery {
        BookReservations.update({ BookReservations.id eq reservationId }) {
            it[status] = newStatus
        } > 0
    }

    suspend fun markNotified(reservationId: UUID): Boolean = tenantDbQuery {
        BookReservations.update({ BookReservations.id eq reservationId }) {
            it[notified] = true
        } > 0
    }

    suspend fun hasActiveReservation(userId: UUID, bookId: UUID): Boolean = tenantDbQuery {
        BookReservations
            .selectAll()
            .where {
                (BookReservations.userId eq userId) and
                (BookReservations.bookId eq bookId) and
                (BookReservations.status inList listOf(ReservationStatus.PENDING, ReservationStatus.AVAILABLE))
            }
            .count() > 0
    }

    suspend fun countPendingByBookId(bookId: UUID): Int = tenantDbQuery {
        BookReservations
            .selectAll()
            .where {
                (BookReservations.bookId eq bookId) and
                (BookReservations.status eq ReservationStatus.PENDING)
            }
            .count()
            .toInt()
    }

    suspend fun findExpired(): List<UUID> = tenantDbQuery {
        val now = LocalDateTime.now()
        BookReservations
            .select(BookReservations.id)
            .where {
                (BookReservations.status eq ReservationStatus.AVAILABLE) and
                (BookReservations.expiryDate less now)
            }
            .map { it[BookReservations.id] }
    }

    private fun mapRowToDto(row: ResultRow, bookAlias: Alias<Books>, userAlias: Alias<Users>): ReservationDto {
        return ReservationDto(
            id = row[BookReservations.id].toString(),
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
            reservedDate = row[BookReservations.reservedDate].toString(),
            expiryDate = row[BookReservations.expiryDate].toString(),
            status = row[BookReservations.status].name,
            notified = row[BookReservations.notified],
            queuePosition = null, // Will be calculated by service
            estimatedAvailabilityDate = null,
            createdAt = row[BookReservations.createdAt].toString()
        )
    }
}
