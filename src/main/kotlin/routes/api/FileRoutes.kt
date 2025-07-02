package com.example.routes.api

import com.example.services.FileService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

fun Route.fileRoutes(fileService: FileService) {

    route("/api/v1/files") {

        // Upload profile picture - FIXED VERSION
        post("/upload/profile") {
            try {
                println("=== MULTIPART DEBUG ===")
                val multipartData = call.receiveMultipart()
                var userId: String? = null
                var fileBytes: ByteArray? = null
                var originalFileName: String? = null
                var partCount = 0

                multipartData.forEachPart { part ->
                    partCount++
                    println("Part $partCount: ${part::class.simpleName}")

                    when (part) {
                        is PartData.FormItem -> {
                            println("  FormItem - name: '${part.name}', value: '${part.value}'")
                            if (part.name == "userId") {
                                userId = part.value
                            }
                            part.dispose()
                        }
                        is PartData.FileItem -> {
                            println("  FileItem - name: '${part.name}', filename: '${part.originalFileName}'")
                            println("  Headers: ${part.headers}")

                            originalFileName = part.originalFileName

                            // Read the file bytes immediately within the IO context
                            fileBytes = withContext(Dispatchers.IO) {
                                try {
                                    val stream = part.streamProvider()
                                    println("Reading stream for file: ${part.originalFileName}")

                                    // Use ByteArrayOutputStream to read all bytes
                                    val buffer = ByteArrayOutputStream()
                                    stream.use { inputStream ->
                                        inputStream.copyTo(buffer)
                                    }
                                    val bytes = buffer.toByteArray()

                                    println("Successfully read ${bytes.size} bytes from stream")
                                    bytes
                                } catch (e: Exception) {
                                    println("Error reading file stream: ${e.message}")
                                    e.printStackTrace()
                                    ByteArray(0) // Return empty array on error
                                }
                            }

                            // Dispose the part after reading
                            part.dispose()
                        }
                        else -> {
                            println("  Other part type: ${part::class.simpleName}")
                            part.dispose()
                        }
                    }
                }

                println("Total parts received: $partCount")
                println("UserId: $userId")
                println("OriginalFileName: $originalFileName")
                println("FileBytes size: ${fileBytes?.size ?: 0}")
                println("======================")

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                    return@post
                }

                if (fileBytes == null || fileBytes!!.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file provided or file is empty"))
                    return@post
                }

                if (originalFileName == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Original filename is missing"))
                    return@post
                }

                val result = fileService.uploadProfilePicture(
                    inputStream = fileBytes!!.inputStream(),
                    originalFileName = originalFileName!!,
                    userId = userId!!
                )

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.BadRequest, result)
                }

            } catch (e: Exception) {
                println("ERROR in upload: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Upload failed: ${e.message}")
                )
            }
        }

        // Upload document - FIXED VERSION
        post("/upload/document") {
            try {
                val multipartData = call.receiveMultipart()
                var userId: String? = null
                var category: String = "general"
                var fileBytes: ByteArray? = null
                var originalFileName: String? = null

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            when (part.name) {
                                "userId" -> userId = part.value
                                "category" -> category = part.value
                            }
                            part.dispose()
                        }
                        is PartData.FileItem -> {
                            originalFileName = part.originalFileName

                            fileBytes = withContext(Dispatchers.IO) {
                                try {
                                    val stream = part.streamProvider()
                                    val buffer = ByteArrayOutputStream()
                                    stream.use { inputStream ->
                                        inputStream.copyTo(buffer)
                                    }
                                    buffer.toByteArray()
                                } catch (e: Exception) {
                                    println("Error reading document stream: ${e.message}")
                                    ByteArray(0)
                                }
                            }

                            part.dispose()
                        }
                        else -> {
                            part.dispose()
                        }
                    }
                }

                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                    return@post
                }

                if (fileBytes == null || fileBytes!!.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "No file provided or file is empty"))
                    return@post
                }

                if (originalFileName == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Original filename is missing"))
                    return@post
                }

                val result = fileService.uploadDocument(
                    inputStream = fileBytes!!.inputStream(),
                    originalFileName = originalFileName!!,
                    userId = userId!!,
                    category = category
                )

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.BadRequest, result)
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Upload failed: ${e.message}")
                )
            }
        }

        // Delete file
        delete("/{filePath...}") {
            try {
                val filePath = call.parameters.getAll("filePath")?.joinToString("/") ?: ""

                if (filePath.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "File path is required"))
                    return@delete
                }

                val result = fileService.deleteFile("/$filePath")

                if (result.success) {
                    call.respond(HttpStatusCode.OK, result)
                } else {
                    call.respond(HttpStatusCode.BadRequest, result)
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Delete failed: ${e.message}")
                )
            }
        }

        // Get shareable link
        /*get("/share/{filePath...}") {
            try {
                val filePath = call.parameters.getAll("filePath")?.joinToString("/") ?: ""

                if (filePath.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "File path is required"))
                    return@get
                }

                val shareableLink = fileService.getShareableLink("/$filePath")

                if (shareableLink != null) {
                    call.respond(HttpStatusCode.OK, mapOf("shareableLink" to shareableLink))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found or unable to create shareable link"))
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get shareable link: ${e.message}")
                )
            }
        }*/

        /*get("/test-dropbox") {
            val result = fileService.testDropboxConnection()
            call.respond(HttpStatusCode.OK, mapOf("result" to result))
        }*/
    }
}