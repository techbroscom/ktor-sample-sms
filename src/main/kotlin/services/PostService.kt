package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreatePostRequest
import com.example.models.dto.PostDto
import com.example.models.dto.UpdatePostRequest
import com.example.repositories.PostRepository
import io.ktor.http.*
import services.S3FileService
import java.io.InputStream

class PostService(
    private val postRepository: PostRepository,
    private val notificationService: NotificationService?,
    private val s3FileService: S3FileService?
) {

    suspend fun createPost(request: CreatePostRequest): PostDto {
        validatePostRequest(request.title, request.content)

        val postId = postRepository.create(request)
        notificationService?.sendSchoolAnnouncement(1, request.title, request.content)
        return getPostById(postId)
    }

    suspend fun createPostWithImage(
        tenantId: String,
        title: String,
        content: String,
        author: String?,
        imageInputStream: InputStream?,
        imageFileName: String?,
        userId: String
    ): PostDto {
        validatePostRequest(title, content)

        var imageUrl: String? = null
        var imageS3Key: String? = null

        // Upload image if provided
        if (imageInputStream != null && imageFileName != null) {
            val uploadResponse = s3FileService?.uploadPostImage(
                tenantId = tenantId,
                inputStream = imageInputStream,
                originalFileName = imageFileName,
                postId = "temp_${System.currentTimeMillis()}",
                userId = userId
            )

            if (uploadResponse?.success == true) {
                imageUrl = uploadResponse.fileUrl
                imageS3Key = uploadResponse.objectKey
            } else {
                throw ApiException(
                    "Image upload failed: ${uploadResponse?.message ?: "Unknown error"}",
                    HttpStatusCode.InternalServerError
                )
            }
        }

        val request = CreatePostRequest(
            title = title,
            content = content,
            author = author,
            imageUrl = imageUrl,
            imageS3Key = imageS3Key
        )

        val postId = postRepository.create(request)
        notificationService?.sendSchoolAnnouncement(1, title, content)
        return getPostById(postId)
    }

    suspend fun getPostById(id: Int): PostDto {
        return postRepository.findById(id)
            ?: throw ApiException("Post not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllPosts(): List<PostDto> {
        return postRepository.getAll()
    }

    suspend fun getAllPosts(limit: Int): List<PostDto> {
        if (limit <= 0) {
            throw ApiException("Limit must be greater than 0", HttpStatusCode.BadRequest)
        }
        return postRepository.getAllWithLimit(limit)
    }

    suspend fun updatePost(id: Int, request: UpdatePostRequest): PostDto {
        validatePostRequest(request.title, request.content)

        val updated = postRepository.update(id, request)
        if (!updated) {
            throw ApiException("Post not found", HttpStatusCode.NotFound)
        }

        return getPostById(id)
    }

    suspend fun updatePostWithImage(
        tenantId: String,
        id: Int,
        title: String,
        content: String,
        author: String?,
        imageInputStream: InputStream?,
        imageFileName: String?,
        userId: String,
        keepExistingImage: Boolean = false
    ): PostDto {
        validatePostRequest(title, content)

        // Get existing post to check for old image
        val existingPost = getPostById(id)

        var imageUrl: String? = if (keepExistingImage) existingPost.imageUrl else null
        var imageS3Key: String? = if (keepExistingImage) existingPost.imageS3Key else null

        // Upload new image if provided
        if (imageInputStream != null && imageFileName != null) {
            // Delete old image if exists
            if (!existingPost.imageS3Key.isNullOrBlank()) {
                s3FileService?.deleteFile(existingPost.imageS3Key)
            }

            val uploadResponse = s3FileService?.uploadPostImage(
                tenantId = tenantId,
                inputStream = imageInputStream,
                originalFileName = imageFileName,
                postId = id.toString(),
                userId = userId
            )

            if (uploadResponse?.success == true) {
                imageUrl = uploadResponse.fileUrl
                imageS3Key = uploadResponse.objectKey
            } else {
                throw ApiException(
                    "Image upload failed: ${uploadResponse?.message ?: "Unknown error"}",
                    HttpStatusCode.InternalServerError
                )
            }
        }

        val request = UpdatePostRequest(
            title = title,
            content = content,
            author = author,
            imageUrl = imageUrl,
            imageS3Key = imageS3Key
        )

        val updated = postRepository.update(id, request)
        if (!updated) {
            throw ApiException("Post not found", HttpStatusCode.NotFound)
        }

        return getPostById(id)
    }

    suspend fun deletePost(id: Int) {
        // Get post to delete associated image
        val post = getPostById(id)

        // Delete image from S3 if exists
        if (!post.imageS3Key.isNullOrBlank()) {
            s3FileService?.deleteFile(post.imageS3Key)
        }

        val deleted = postRepository.delete(id)
        if (!deleted) {
            throw ApiException("Post not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getPostsByAuthor(author: String): List<PostDto> {
        if (author.isBlank()) {
            throw ApiException("Author name cannot be empty", HttpStatusCode.BadRequest)
        }
        return postRepository.findByAuthor(author)
    }

    suspend fun searchPostsByTitle(searchTerm: String): List<PostDto> {
        if (searchTerm.isBlank()) {
            throw ApiException("Search term cannot be empty", HttpStatusCode.BadRequest)
        }
        return postRepository.searchByTitle(searchTerm)
    }

    private fun validatePostRequest(title: String, content: String) {
        when {
            title.isBlank() -> throw ApiException("Post title cannot be empty", HttpStatusCode.BadRequest)
            title.length > 255 -> throw ApiException("Post title is too long (max 255 characters)", HttpStatusCode.BadRequest)
            content.isBlank() -> throw ApiException("Post content cannot be empty", HttpStatusCode.BadRequest)
        }
    }
}