package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreatePostRequest
import com.example.models.dto.PostDto
import com.example.models.dto.UpdatePostRequest
import com.example.repositories.PostImageRepository
import com.example.repositories.PostRepository
import io.ktor.http.*
import services.S3FileService
import java.io.InputStream

class PostService(
    private val postRepository: PostRepository,
    private val postImageRepository: PostImageRepository,
    private val notificationService: NotificationService?,
    private val s3FileService: S3FileService?
) {

    suspend fun createPost(request: CreatePostRequest): PostDto {
        validatePostRequest(request.title, request.content)

        val postId = postRepository.create(request)
        notificationService?.sendSchoolAnnouncement(1, request.title, request.content)
        return getPostById(postId)
    }

    suspend fun createPostWithImages(
        tenantId: String,
        title: String,
        content: String,
        author: String?,
        images: List<Pair<InputStream, String>>, // List of (inputStream, fileName)
        userId: String
    ): PostDto {
        validatePostRequest(title, content)

        // Create the post first
        val request = CreatePostRequest(
            title = title,
            content = content,
            author = author
        )
        val postId = postRepository.create(request)

        // Upload and attach images
        images.forEachIndexed { index, (imageInputStream, imageFileName) ->
            val uploadResponse = s3FileService?.uploadPostImage(
                tenantId = tenantId,
                inputStream = imageInputStream,
                originalFileName = imageFileName,
                postId = postId.toString(),
                userId = userId
            )

            if (uploadResponse?.success == true) {
                postImageRepository.create(
                    postId = postId,
                    imageUrl = "", // Don't store full URL - will be generated dynamically from S3 key
                    imageS3Key = uploadResponse.objectKey ?: "",
                    displayOrder = index
                )
            } else {
                // If any image fails, we might want to rollback or continue
                throw ApiException(
                    "Image upload failed: ${uploadResponse?.message ?: "Unknown error"}",
                    HttpStatusCode.InternalServerError
                )
            }
        }

        notificationService?.sendSchoolAnnouncement(1, title, content)
        return getPostById(postId)
    }

    suspend fun getPostById(id: Int): PostDto {
        val post = postRepository.findById(id)
            ?: throw ApiException("Post not found", HttpStatusCode.NotFound)

        // Load images for the post
        val images = postImageRepository.findByPostId(id)
        return post.copy(images = images)
    }

    suspend fun getAllPosts(): List<PostDto> {
        val posts = postRepository.getAll()
        return posts.map { post ->
            val images = postImageRepository.findByPostId(post.id ?: 0)
            post.copy(images = images)
        }
    }

    suspend fun getAllPosts(limit: Int): List<PostDto> {
        if (limit <= 0) {
            throw ApiException("Limit must be greater than 0", HttpStatusCode.BadRequest)
        }
        val posts = postRepository.getAllWithLimit(limit)
        return posts.map { post ->
            val images = postImageRepository.findByPostId(post.id ?: 0)
            post.copy(images = images)
        }
    }

    suspend fun updatePost(id: Int, request: UpdatePostRequest): PostDto {
        validatePostRequest(request.title, request.content)

        val updated = postRepository.update(id, request)
        if (!updated) {
            throw ApiException("Post not found", HttpStatusCode.NotFound)
        }

        return getPostById(id)
    }

    suspend fun updatePostWithImages(
        tenantId: String,
        id: Int,
        title: String,
        content: String,
        author: String?,
        newImages: List<Pair<InputStream, String>>, // New images to add
        userId: String,
        replaceExistingImages: Boolean = false
    ): PostDto {
        validatePostRequest(title, content)

        // Update post basic info
        val request = UpdatePostRequest(
            title = title,
            content = content,
            author = author
        )
        val updated = postRepository.update(id, request)
        if (!updated) {
            throw ApiException("Post not found", HttpStatusCode.NotFound)
        }

        // Handle images
        if (replaceExistingImages) {
            // Delete old images from S3 and database
            val existingImages = postImageRepository.findByPostId(id)
            existingImages.forEach { image ->
                s3FileService?.deleteFile(image.imageS3Key)
            }
            postImageRepository.deleteByPostId(id)
        }

        // Upload and attach new images
        val currentImageCount = if (replaceExistingImages) 0 else postImageRepository.findByPostId(id).size
        newImages.forEachIndexed { index, (imageInputStream, imageFileName) ->
            val uploadResponse = s3FileService?.uploadPostImage(
                tenantId = tenantId,
                inputStream = imageInputStream,
                originalFileName = imageFileName,
                postId = id.toString(),
                userId = userId
            )

            if (uploadResponse?.success == true) {
                postImageRepository.create(
                    postId = id,
                    imageUrl = "", // Don't store full URL - will be generated dynamically from S3 key
                    imageS3Key = uploadResponse.objectKey ?: "",
                    displayOrder = currentImageCount + index
                )
            } else {
                throw ApiException(
                    "Image upload failed: ${uploadResponse?.message ?: "Unknown error"}",
                    HttpStatusCode.InternalServerError
                )
            }
        }

        return getPostById(id)
    }

    suspend fun deletePostImage(postId: Int, imageId: Int) {
        val image = postImageRepository.findById(imageId)
            ?: throw ApiException("Image not found", HttpStatusCode.NotFound)

        if (image.postId != postId) {
            throw ApiException("Image does not belong to this post", HttpStatusCode.BadRequest)
        }

        // Delete from S3
        s3FileService?.deleteFile(image.imageS3Key)

        // Delete from database
        postImageRepository.deleteById(imageId)
    }

    suspend fun deletePost(id: Int) {
        // Get post images to delete from S3
        val images = postImageRepository.findByPostId(id)

        // Delete all images from S3
        images.forEach { image ->
            s3FileService?.deleteFile(image.imageS3Key)
        }

        // Delete images from database
        postImageRepository.deleteByPostId(id)

        // Delete post
        val deleted = postRepository.delete(id)
        if (!deleted) {
            throw ApiException("Post not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getPostsByAuthor(author: String): List<PostDto> {
        if (author.isBlank()) {
            throw ApiException("Author name cannot be empty", HttpStatusCode.BadRequest)
        }
        val posts = postRepository.findByAuthor(author)
        return posts.map { post ->
            val images = postImageRepository.findByPostId(post.id ?: 0)
            post.copy(images = images)
        }
    }

    suspend fun searchPostsByTitle(searchTerm: String): List<PostDto> {
        if (searchTerm.isBlank()) {
            throw ApiException("Search term cannot be empty", HttpStatusCode.BadRequest)
        }
        val posts = postRepository.searchByTitle(searchTerm)
        return posts.map { post ->
            val images = postImageRepository.findByPostId(post.id ?: 0)
            post.copy(images = images)
        }
    }

    private fun validatePostRequest(title: String, content: String) {
        when {
            title.isBlank() -> throw ApiException("Post title cannot be empty", HttpStatusCode.BadRequest)
            title.length > 255 -> throw ApiException("Post title is too long (max 255 characters)", HttpStatusCode.BadRequest)
            content.isBlank() -> throw ApiException("Post content cannot be empty", HttpStatusCode.BadRequest)
        }
    }
}
