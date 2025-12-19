package com.example.repositories

import com.example.database.tables.PostImages
import com.example.models.dto.PostImageDto
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import java.time.LocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import services.S3FileService

class PostImageRepository(
    private val s3FileService: S3FileService?
) {

    suspend fun create(
        postId: Int,
        imageUrl: String,
        imageS3Key: String,
        displayOrder: Int
    ): Int = tenantDbQuery {
        PostImages.insert {
            it[PostImages.postId] = postId
            it[PostImages.imageUrl] = imageUrl
            it[PostImages.imageS3Key] = imageS3Key
            it[PostImages.displayOrder] = displayOrder
            it[createdAt] = LocalDateTime.now()
        }[PostImages.id]
    }

    suspend fun findByPostId(postId: Int): List<PostImageDto> = tenantDbQuery {
        PostImages.selectAll()
            .where { PostImages.postId eq postId }
            .orderBy(PostImages.displayOrder to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findById(id: Int): PostImageDto? = tenantDbQuery {
        PostImages.selectAll()
            .where { PostImages.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun deleteByPostId(postId: Int): Int = tenantDbQuery {
        PostImages.deleteWhere { PostImages.postId eq postId }
    }

    suspend fun deleteById(id: Int): Boolean = tenantDbQuery {
        PostImages.deleteWhere { PostImages.id eq id } > 0
    }

    suspend fun deleteByS3Key(s3Key: String): Boolean = tenantDbQuery {
        PostImages.deleteWhere { PostImages.imageS3Key eq s3Key } > 0
    }

    suspend fun updateDisplayOrder(id: Int, newOrder: Int): Boolean = tenantDbQuery {
        PostImages.update({ PostImages.id eq id }) {
            it[displayOrder] = newOrder
        } > 0
    }

    private suspend fun mapRowToDto(row: ResultRow): PostImageDto {
        val imageS3Key = row[PostImages.imageS3Key]
        // Generate URL dynamically from S3 key instead of using stored URL
        val imageUrl = s3FileService?.generateSignedUrlByKey(imageS3Key, expirationMinutes = 60) ?: ""

        return PostImageDto(
            id = row[PostImages.id],
            postId = row[PostImages.postId],
            imageUrl = imageUrl,
            imageS3Key = imageS3Key,
            displayOrder = row[PostImages.displayOrder],
            createdAt = row[PostImages.createdAt].toString()
        )
    }
}
