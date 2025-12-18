package routes.api

import services.S3FileService
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.*

fun Route.s3FileRoutes(fileService: S3FileService) {

    route("/api/v1/s3-files") {

        // Upload profile picture
        post("/upload/profile") {
            try {
                val tenantId = call.request.headers["X-Tenant"] ?: "default"
                println("=== MULTIPART DEBUG ===")
                println("Tenant: $tenantId")

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
                                    println("Error reading file stream: ${e.message}")
                                    e.printStackTrace()
                                    ByteArray(0)
                                }
                            }
                            part.dispose()
                        }
                        else -> {
                            println("  Other part type: ${part::class.simpleName}")
                            part.dispose()
                        }
                    }
                }

                if (userId == null || originalFileName == null || fileBytes == null || fileBytes!!.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required data"))
                    return@post
                }

                val result = fileService.uploadProfilePicture(
                    tenantId = tenantId,
                    userId = userId!!,
                    fileName = originalFileName!!,
                    bytes = fileBytes!!
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

        // Upload document
        post("/upload/document") {
            try {
                val tenantId = call.request.headers["X-Tenant"] ?: "default"
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

                if (userId == null || originalFileName == null || fileBytes == null || fileBytes!!.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing required data"))
                    return@post
                }

                val result = fileService.uploadDocument(
                    tenantId = tenantId,
                    userId = userId!!,
                    category = category,
                    originalName = originalFileName!!,
                    bytes = fileBytes!!
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

        // Delete file by object key
        delete("/{filePath...}") {
            try {
                val filePath = call.parameters.getAll("filePath")?.joinToString("/") ?: ""

                if (filePath.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "File path is required"))
                    return@delete
                }

                val result = fileService.deleteFile(filePath)

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

        // Get signed URL for a file by ID
        get("/signed-url/{fileId}") {
            try {
                val fileId = call.parameters["fileId"]
                if (fileId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "fileId is required"))
                    return@get
                }

                val fileUuid = try {
                    UUID.fromString(fileId)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid fileId format"))
                    return@get
                }

                val expirationMinutes = call.request.queryParameters["expiration"]?.toLongOrNull() ?: 60

                val signedUrl = fileService.generateSignedUrl(fileUuid, expirationMinutes)

                if (signedUrl != null) {
                    call.respond(HttpStatusCode.OK, mapOf(
                        "signedUrl" to signedUrl,
                        "expiresIn" to "$expirationMinutes minutes"
                    ))
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get signed URL: ${e.message}")
                )
            }
        }

        // Get file metadata by ID
        get("/metadata/{fileId}") {
            try {
                val fileId = call.parameters["fileId"]
                if (fileId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "fileId is required"))
                    return@get
                }

                val fileUuid = try {
                    UUID.fromString(fileId)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid fileId format"))
                    return@get
                }

                val fileRecord = fileService.getFileById(fileUuid)

                if (fileRecord != null) {
                    call.respond(HttpStatusCode.OK, fileRecord)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "File not found"))
                }

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get file metadata: ${e.message}")
                )
            }
        }

        // Get all files by user
        get("/user/{userId}") {
            try {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                    return@get
                }

                val userUuid = try {
                    UUID.fromString(userId)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid userId format"))
                    return@get
                }

                val files = fileService.getFilesByUser(userUuid)

                call.respond(HttpStatusCode.OK, mapOf(
                    "files" to files,
                    "count" to files.size
                ))

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get files: ${e.message}")
                )
            }
        }

        // Get storage usage by user
        get("/storage/user/{userId}") {
            try {
                val userId = call.parameters["userId"]
                if (userId == null) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "userId is required"))
                    return@get
                }

                val userUuid = try {
                    UUID.fromString(userId)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid userId format"))
                    return@get
                }

                val totalBytes = fileService.getTotalStorageByUser(userUuid)
                val totalMB = totalBytes / (1024.0 * 1024.0)

                call.respond(HttpStatusCode.OK, mapOf(
                    "totalBytes" to totalBytes,
                    "totalMB" to String.format("%.2f", totalMB)
                ))

            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    mapOf("error" to "Failed to get storage usage: ${e.message}")
                )
            }
        }

        // Test storage connection
        get("/test-storage") {
            val result = fileService.testStorageConnection()
            call.respond(HttpStatusCode.OK, mapOf("result" to result))
        }
    }
}
