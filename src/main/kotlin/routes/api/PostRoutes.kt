package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreatePostRequest
import com.example.models.dto.UpdatePostRequest
import com.example.models.responses.ApiResponse
import com.example.services.PostService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

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

        // Create post with image (multipart)
        post("/with-image") {
            try {
                val tenantId = call.request.headers["X-Tenant"] ?: "default"
                val multipartData = call.receiveMultipart()

                var title: String? = null
                var content: String? = null
                var author: String? = null
                var userId: String? = null
                var imageBytes: ByteArray? = null
                var imageFileName: String? = null

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "title" -> title = part.value
                                "content" -> content = part.value
                                "author" -> author = part.value
                                "userId" -> userId = part.value
                            }
                            part.dispose()
                        }
                        is PartData.FileItem -> {
                            if (part.name == "image") {
                                imageFileName = part.originalFileName
                                imageBytes = withContext(Dispatchers.IO) {
                                    try {
                                        val stream = part.streamProvider()
                                        val buffer = ByteArrayOutputStream()
                                        stream.use { inputStream ->
                                            inputStream.copyTo(buffer)
                                        }
                                        buffer.toByteArray()
                                    } catch (e: Exception) {
                                        println("Error reading image stream: ${e.message}")
                                        ByteArray(0)
                                    }
                                }
                            }
                            part.dispose()
                        }
                        else -> {
                            part.dispose()
                        }
                    }
                }

                if (title == null || content == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                        success = false,
                        message = "title, content, and userId are required"
                    ))
                    return@post
                }

                val post = postService.createPostWithImage(
                    tenantId = tenantId,
                    title = title!!,
                    content = content!!,
                    author = author,
                    imageInputStream = imageBytes?.inputStream(),
                    imageFileName = imageFileName,
                    userId = userId!!
                )

                call.respond(HttpStatusCode.Created, ApiResponse(
                    success = true,
                    data = post,
                    message = "Post created successfully"
                ))

            } catch (e: Exception) {
                println("ERROR creating post with image: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        message = "Failed to create post: ${e.message}"
                    )
                )
            }
        }

        // Update post with image (multipart)
        put("/{id}/with-image") {
            try {
                val tenantId = call.request.headers["X-Tenant"] ?: "default"
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw ApiException("Invalid post ID", HttpStatusCode.BadRequest)

                val multipartData = call.receiveMultipart()

                var title: String? = null
                var content: String? = null
                var author: String? = null
                var userId: String? = null
                var imageBytes: ByteArray? = null
                var imageFileName: String? = null
                var keepExistingImage: Boolean = true

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "title" -> title = part.value
                                "content" -> content = part.value
                                "author" -> author = part.value
                                "userId" -> userId = part.value
                                "keepExistingImage" -> keepExistingImage = part.value.toBoolean()
                            }
                            part.dispose()
                        }
                        is PartData.FileItem -> {
                            if (part.name == "image") {
                                imageFileName = part.originalFileName
                                imageBytes = withContext(Dispatchers.IO) {
                                    try {
                                        val stream = part.streamProvider()
                                        val buffer = ByteArrayOutputStream()
                                        stream.use { inputStream ->
                                            inputStream.copyTo(buffer)
                                        }
                                        buffer.toByteArray()
                                    } catch (e: Exception) {
                                        println("Error reading image stream: ${e.message}")
                                        ByteArray(0)
                                    }
                                }
                            }
                            part.dispose()
                        }
                        else -> {
                            part.dispose()
                        }
                    }
                }

                if (title == null || content == null || userId == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                        success = false,
                        message = "title, content, and userId are required"
                    ))
                    return@put
                }

                val post = postService.updatePostWithImage(
                    tenantId = tenantId,
                    id = id,
                    title = title!!,
                    content = content!!,
                    author = author,
                    imageInputStream = imageBytes?.inputStream(),
                    imageFileName = imageFileName,
                    userId = userId!!,
                    keepExistingImage = keepExistingImage
                )

                call.respond(ApiResponse(
                    success = true,
                    data = post,
                    message = "Post updated successfully"
                ))

            } catch (e: Exception) {
                println("ERROR updating post with image: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        message = "Failed to update post: ${e.message}"
                    )
                )
            }
        }
    }
}