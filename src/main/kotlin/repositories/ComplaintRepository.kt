package com.example.repositories

import com.example.database.tables.Complaints
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ComplaintRepository {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    suspend fun create(request: CreateComplaintRequest): String = tenantDbQuery {
        val complaintId = UUID.randomUUID().toString()
        val now = LocalDateTime.now()

        Complaints.insert {
            it[id] = complaintId
            it[title] = request.title
            it[content] = request.content
            it[author] = request.author
            it[category] = request.category
            it[status] = "Open" // Default status
            it[isAnonymous] = request.isAnonymous
            it[createdAt] = now
            it[comments] = json.encodeToString(ListSerializer(CommentDto.serializer()), emptyList())
        }

        complaintId
    }

    suspend fun findById(id: String): ComplaintDto? = tenantDbQuery {
        Complaints.selectAll()
            .where { Complaints.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<ComplaintDto> = tenantDbQuery {
        Complaints.selectAll()
            .orderBy(Complaints.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAuthor(authorId: String): List<ComplaintDto> = tenantDbQuery {
        Complaints.selectAll()
            .where { Complaints.author eq authorId }
            .orderBy(Complaints.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateComplaintRequest): Boolean = tenantDbQuery {
        Complaints.update({ Complaints.id eq id }) {
            it[title] = request.title
            it[content] = request.content
            it[category] = request.category
            it[status] = request.status
        } > 0
    }

    suspend fun delete(id: String): Boolean = tenantDbQuery {
        Complaints.deleteWhere { Complaints.id eq id } > 0
    }

    suspend fun updateStatus(id: String, status: String): Boolean = tenantDbQuery {
        Complaints.update({ Complaints.id eq id }) {
            it[Complaints.status] = status
        } > 0
    }

    suspend fun addComment(id: String, comment: CommentDto): Boolean = tenantDbQuery {
        // First, get the current complaint
        val currentComplaint = Complaints.selectAll()
            .where { Complaints.id eq id }
            .singleOrNull()
            ?: return@tenantDbQuery false

        // Deserialize current comments
        val currentCommentsJson = currentComplaint[Complaints.comments]
        val currentComments = try {
            json.decodeFromString(ListSerializer(CommentDto.serializer()), currentCommentsJson)
        } catch (e: Exception) {
            emptyList<CommentDto>()
        }

        // Add new comment
        val updatedComments = currentComments + comment
        val updatedCommentsJson = json.encodeToString(ListSerializer(CommentDto.serializer()), updatedComments)

        // Update the complaint
        Complaints.update({ Complaints.id eq id }) {
            it[comments] = updatedCommentsJson
        } > 0
    }

    suspend fun findByCategory(category: String): List<ComplaintDto> = tenantDbQuery {
        Complaints.selectAll()
            .where { Complaints.category eq category }
            .orderBy(Complaints.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStatus(status: String): List<ComplaintDto> = tenantDbQuery {
        Complaints.selectAll()
            .where { Complaints.status eq status }
            .orderBy(Complaints.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    private fun mapRowToDto(row: ResultRow): ComplaintDto {
        val commentsJson = row[Complaints.comments]
        val comments = try {
            json.decodeFromString(ListSerializer(CommentDto.serializer()), commentsJson)
        } catch (e: Exception) {
            emptyList<CommentDto>()
        }

        return ComplaintDto(
            id = row[Complaints.id],
            title = row[Complaints.title],
            content = row[Complaints.content],
            author = row[Complaints.author],
            category = row[Complaints.category],
            status = row[Complaints.status],
            isAnonymous = row[Complaints.isAnonymous],
            createdAt = row[Complaints.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            comments = comments
        )
    }
}