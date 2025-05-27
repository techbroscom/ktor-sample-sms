package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreatePostRequest
import com.example.models.dto.PostDto
import com.example.models.dto.UpdatePostRequest
import com.example.repositories.PostRepository
import io.ktor.http.*

class PostService(private val postRepository: PostRepository) {

    suspend fun createPost(request: CreatePostRequest): PostDto {
        validatePostRequest(request.title, request.content)

        val postId = postRepository.create(request)
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

    suspend fun deletePost(id: Int) {
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