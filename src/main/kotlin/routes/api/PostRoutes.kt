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

        // Get posts with filters and pagination
        get("/filter") {
            val author = call.request.queryParameters["author"]
            val search = call.request.queryParameters["search"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val paginatedPosts = postService.getPostsWithFilters(author, search, page, pageSize)
            call.respond(paginatedPosts)
        }

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

        // Create post (JSON - no images)
        post {
            val request = call.receive<CreatePostRequest>()
            val post = postService.createPost(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = post,
                message = "Post created successfully"
            ))
        }

        // Update post (JSON - no images)
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

        // Create post with multiple images (multipart)
        post("/with-images") {
            try {
                val tenantId = call.request.headers["X-Tenant"] ?: "default"
                val multipartData = call.receiveMultipart()

                var title: String? = null
                var content: String? = null
                var author: String? = null
                var userId: String? = null
                val imagesList = mutableListOf<Pair<ByteArray, String>>() // (bytes, fileName)

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
                            if (part.name == "images" || part.name == "images[]") {
                                val fileName = part.originalFileName ?: "unknown"
                                val imageBytes = withContext(Dispatchers.IO) {
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
                                if (imageBytes.isNotEmpty()) {
                                    imagesList.add(imageBytes to fileName)
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

                // Convert ByteArray list to InputStream list
                val images = imagesList.map { (bytes, fileName) ->
                    bytes.inputStream() to fileName
                }

                val post = postService.createPostWithImages(
                    tenantId = tenantId,
                    title = title!!,
                    content = content!!,
                    author = author,
                    images = images,
                    userId = userId!!
                )

                call.respond(HttpStatusCode.Created, ApiResponse(
                    success = true,
                    data = post,
                    message = "Post created successfully with ${images.size} image(s)"
                ))

            } catch (e: Exception) {
                println("ERROR creating post with images: ${e.message}")
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

        // Update post with multiple images (multipart)
        put("/{id}/with-images") {
            try {
                val tenantId = call.request.headers["X-Tenant"] ?: "default"
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: throw ApiException("Invalid post ID", HttpStatusCode.BadRequest)

                val multipartData = call.receiveMultipart()

                var title: String? = null
                var content: String? = null
                var author: String? = null
                var userId: String? = null
                var replaceExistingImages: Boolean = false
                val imagesList = mutableListOf<Pair<ByteArray, String>>()

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "title" -> title = part.value
                                "content" -> content = part.value
                                "author" -> author = part.value
                                "userId" -> userId = part.value
                                "replaceExistingImages" -> replaceExistingImages = part.value.toBoolean()
                            }
                            part.dispose()
                        }
                        is PartData.FileItem -> {
                            if (part.name == "images" || part.name == "images[]") {
                                val fileName = part.originalFileName ?: "unknown"
                                val imageBytes = withContext(Dispatchers.IO) {
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
                                if (imageBytes.isNotEmpty()) {
                                    imagesList.add(imageBytes to fileName)
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

                // Convert ByteArray list to InputStream list
                val newImages = imagesList.map { (bytes, fileName) ->
                    bytes.inputStream() to fileName
                }

                val post = postService.updatePostWithImages(
                    tenantId = tenantId,
                    id = id,
                    title = title!!,
                    content = content!!,
                    author = author,
                    newImages = newImages,
                    userId = userId!!,
                    replaceExistingImages = replaceExistingImages
                )

                call.respond(ApiResponse(
                    success = true,
                    data = post,
                    message = "Post updated successfully${if (newImages.isNotEmpty()) " with ${newImages.size} new image(s)" else ""}"
                ))

            } catch (e: Exception) {
                println("ERROR updating post with images: ${e.message}")
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

        // Delete a specific image from a post
        delete("/{postId}/images/{imageId}") {
            try {
                val postId = call.parameters["postId"]?.toIntOrNull()
                    ?: throw ApiException("Invalid post ID", HttpStatusCode.BadRequest)
                val imageId = call.parameters["imageId"]?.toIntOrNull()
                    ?: throw ApiException("Invalid image ID", HttpStatusCode.BadRequest)

                postService.deletePostImage(postId, imageId)
                call.respond(ApiResponse<Unit>(
                    success = true,
                    message = "Image deleted successfully"
                ))

            } catch (e: Exception) {
                println("ERROR deleting post image: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        message = "Failed to delete image: ${e.message}"
                    )
                )
            }
        }
    }
}
