package com.example.repositories

import com.example.database.tables.Posts
import com.example.models.dto.CreatePostRequest
import com.example.models.dto.PostDto
import com.example.models.dto.UpdatePostRequest
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime

class PostRepository {

    suspend fun create(request: CreatePostRequest): Int = tenantDbQuery {
        Posts.insert {
            it[title] = request.title
            it[content] = request.content
            it[author] = request.author
            it[createdAt] = LocalDateTime.now()
        }[Posts.id]
    }

    suspend fun findById(id: Int): PostDto? = tenantDbQuery {
        Posts.selectAll()
            .where { Posts.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun getAll(): List<PostDto> = tenantDbQuery {
        Posts.selectAll()
            .orderBy(Posts.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun getAllWithLimit(limit: Int): List<PostDto> = tenantDbQuery {
        Posts.selectAll()
            .orderBy(Posts.createdAt to SortOrder.DESC)
            .limit(limit)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: Int, request: UpdatePostRequest): Boolean = tenantDbQuery {
        Posts.update({ Posts.id eq id }) {
            it[title] = request.title
            it[content] = request.content
            it[author] = request.author
        } > 0
    }

    suspend fun delete(id: Int): Boolean = tenantDbQuery {
        Posts.deleteWhere { Posts.id eq id } > 0
    }

    suspend fun findByAuthor(author: String): List<PostDto> = tenantDbQuery {
        Posts.selectAll()
            .where { Posts.author eq author }
            .orderBy(Posts.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun searchByTitle(searchTerm: String): List<PostDto> = tenantDbQuery {
        Posts.selectAll()
            .where { Posts.title.lowerCase() like "%${searchTerm.lowercase()}%" }
            .orderBy(Posts.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    private fun mapRowToDto(row: ResultRow): PostDto {
        return PostDto(
            id = row[Posts.id],
            title = row[Posts.title],
            content = row[Posts.content],
            author = row[Posts.author],
            createdAt = row[Posts.createdAt].toString()
        )
    }
}