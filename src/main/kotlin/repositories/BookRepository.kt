package com.example.repositories

import com.example.database.tables.BookStatus
import com.example.database.tables.Books
import com.example.database.tables.Users
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class BookRepository {

    suspend fun create(request: CreateBookRequest, addedBy: UUID): UUID = tenantDbQuery {
        val bookId = UUID.randomUUID()
        Books.insert {
            it[id] = bookId
            it[isbn] = request.isbn
            it[title] = request.title
            it[author] = request.author
            it[publisher] = request.publisher
            it[publicationYear] = request.publicationYear
            it[edition] = request.edition
            it[language] = request.language
            it[category] = request.category
            it[subCategory] = request.subCategory
            it[totalCopies] = request.totalCopies
            it[availableCopies] = request.totalCopies
            it[shelfLocation] = request.shelfLocation
            it[coverImageUrl] = request.coverImageUrl
            it[description] = request.description
            it[price] = request.price?.let { p -> BigDecimal(p) }
            it[status] = BookStatus.AVAILABLE
            it[Books.addedBy] = addedBy
            it[createdAt] = LocalDateTime.now()
        }
        bookId
    }

    suspend fun findById(bookId: UUID): BookDto? = tenantDbQuery {
        val addedByAlias = Users.alias("added_by_user")

        Books
            .join(addedByAlias, JoinType.INNER, Books.addedBy, addedByAlias[Users.id])
            .selectAll()
            .where { Books.id eq bookId }
            .map { mapRowToDto(it, addedByAlias) }
            .singleOrNull()
    }

    suspend fun search(request: BookSearchRequest): Pair<List<BookDto>, Long> = tenantDbQuery {
        val addedByAlias = Users.alias("added_by_user")

        var query = Books
            .join(addedByAlias, JoinType.INNER, Books.addedBy, addedByAlias[Users.id])
            .selectAll()

        // Apply filters
        request.searchQuery?.let { searchTerm ->
            query = query.andWhere {
                (Books.title like "%$searchTerm%") or
                (Books.author like "%$searchTerm%") or
                (Books.isbn like "%$searchTerm%")
            }
        }

        request.category?.let {
            query = query.andWhere { Books.category eq it }
        }

        request.language?.let {
            query = query.andWhere { Books.language eq it }
        }

        if (request.availableOnly) {
            query = query.andWhere { Books.availableCopies greater 0 }
        }

        val total = query.count()
        val offset = ((request.page - 1) * request.pageSize).toLong()

        val books = query
            .limit(request.pageSize, offset)
            .orderBy(Books.title to SortOrder.ASC)
            .map { mapRowToDto(it, addedByAlias) }

        Pair(books, total)
    }

    suspend fun update(bookId: UUID, request: UpdateBookRequest): Boolean = tenantDbQuery {
        Books.update({ Books.id eq bookId }) {
            request.isbn?.let { v -> it[isbn] = v }
            request.title?.let { v -> it[title] = v }
            request.author?.let { v -> it[author] = v }
            request.publisher?.let { v -> it[publisher] = v }
            request.publicationYear?.let { v -> it[publicationYear] = v }
            request.edition?.let { v -> it[edition] = v }
            request.language?.let { v -> it[language] = v }
            request.category?.let { v -> it[category] = v }
            request.subCategory?.let { v -> it[subCategory] = v }
            request.totalCopies?.let { v -> it[totalCopies] = v }
            request.shelfLocation?.let { v -> it[shelfLocation] = v }
            request.coverImageUrl?.let { v -> it[coverImageUrl] = v }
            request.description?.let { v -> it[description] = v }
            request.price?.let { v -> it[price] = BigDecimal(v) }
            request.status?.let { v -> it[status] = BookStatus.valueOf(v) }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(bookId: UUID): Boolean = tenantDbQuery {
        Books.deleteWhere { Books.id eq bookId } > 0
    }

    suspend fun updateAvailability(bookId: UUID, change: Int): Boolean = tenantDbQuery {
        Books.update({ Books.id eq bookId }) {
            with(SqlExpressionBuilder) {
                it[availableCopies] = availableCopies + change
            }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun getAllCategories(): List<String> = tenantDbQuery {
        Books
            .select(Books.category)
            .withDistinct()
            .map { it[Books.category] }
            .sorted()
    }

    suspend fun findByIsbn(isbn: String): BookDto? = tenantDbQuery {
        val addedByAlias = Users.alias("added_by_user")

        Books
            .join(addedByAlias, JoinType.INNER, Books.addedBy, addedByAlias[Users.id])
            .selectAll()
            .where { Books.isbn eq isbn }
            .map { mapRowToDto(it, addedByAlias) }
            .singleOrNull()
    }

    private fun mapRowToDto(row: ResultRow, addedByAlias: Alias<Users>): BookDto {
        return BookDto(
            id = row[Books.id].toString(),
            isbn = row[Books.isbn],
            title = row[Books.title],
            author = row[Books.author],
            publisher = row[Books.publisher],
            publicationYear = row[Books.publicationYear],
            edition = row[Books.edition],
            language = row[Books.language],
            category = row[Books.category],
            subCategory = row[Books.subCategory],
            totalCopies = row[Books.totalCopies],
            availableCopies = row[Books.availableCopies],
            shelfLocation = row[Books.shelfLocation],
            coverImageUrl = row[Books.coverImageUrl],
            description = row[Books.description],
            price = row[Books.price]?.toString(),
            status = row[Books.status].name,
            addedBy = UserSummaryDto(
                id = row[addedByAlias[Users.id]].toString(),
                firstName = row[addedByAlias[Users.firstName]],
                lastName = row[addedByAlias[Users.lastName]],
                email = row[addedByAlias[Users.email]],
                role = row[addedByAlias[Users.role]].name
            ),
            createdAt = row[Books.createdAt].toString(),
            updatedAt = row[Books.updatedAt]?.toString()
        )
    }
}
