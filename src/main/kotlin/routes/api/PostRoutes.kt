package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreatePostRequest
import com.example.models.dto.UpdatePostRequest
import com.example.models.responses.ApiResponse
import com.example.services.PostService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.postRoutes(postService: PostService) {
    route("/api/v1/posts") {

        // Get all posts (with optional limit)
        get {
            val limit = call.request.queryParameters["limit"]?.toIntOrNull()
            val posts = if (limit != null) {
                postService.getAllPosts(limit)
            } else {
                postService.getAllPosts()
            }
            call.respond(ApiResponse(
                success = true,
                data = posts
            ))
        }

        // Search posts by title
        get("/search") {
            val searchTerm = call.request.queryParameters["q"]
                ?: throw ApiException("Search query parameter 'q' is required", HttpStatusCode.BadRequest)

            val posts = postService.searchPostsByTitle(searchTerm)
            call.respond(ApiResponse(
                success = true,
                data = posts
            ))
        }

        // Get posts by author
        get("/author/{author}") {
            val author = call.parameters["author"]
                ?: throw ApiException("Author parameter is required", HttpStatusCode.BadRequest)

            val posts = postService.getPostsByAuthor(author)
            call.respond(ApiResponse(
                success = true,
                data = posts
            ))
        }

        // Get post by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid post ID", HttpStatusCode.BadRequest)

            val post = postService.getPostById(id)
            call.respond(ApiResponse(
                success = true,
                data = post
            ))
        }

        // Create post
        post {
            val request = call.receive<CreatePostRequest>()
            val post = postService.createPost(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = post,
                message = "Post created successfully"
            ))
        }

        // Update post
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid post ID", HttpStatusCode.BadRequest)

            val request = call.receive<UpdatePostRequest>()
            val post = postService.updatePost(id, request)
            call.respond(ApiResponse(
                success = true,
                data = post,
                message = "Post updated successfully"
            ))
        }

        // Delete post
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid post ID", HttpStatusCode.BadRequest)

            postService.deletePost(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Post deleted successfully"
            ))
        }
    }
}