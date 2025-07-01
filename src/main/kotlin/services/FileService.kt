package com.example.services

import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.WriteMode
import com.dropbox.core.v2.sharing.SharedLinkMetadata
import com.example.config.DropboxConfig
import com.example.models.FileUploadResponse
import com.example.models.FileDeleteResponse
import com.example.models.UploadedFile
import org.apache.tika.Tika
import java.io.InputStream
import java.util.*

class FileService(private val dropboxConfig: DropboxConfig) {

    private val dbxClient: DbxClientV2
    private val tika = Tika()

    init {
        val config = DbxRequestConfig.newBuilder("school-management/1.0").build()
        dbxClient = DbxClientV2(config, dropboxConfig.accessToken)
    }

    companion object {
        private val ALLOWED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
        )
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        private const val PROFILE_PICS_FOLDER = "profile-pics"
        private const val DOCUMENTS_FOLDER = "documents"
    }

    /**
     * Upload profile picture
     */
    suspend fun uploadProfilePicture(
        inputStream: InputStream,
        originalFileName: String,
        userId: String
    ): FileUploadResponse {
        return try {
            println("=== UPLOAD DEBUG ===")
            println("Starting profile picture upload for user: $userId")
            println("Original filename: $originalFileName")

            val fileBytes = inputStream.readBytes()
            println("Read ${fileBytes.size} bytes from input stream")

            // Validate file
            val validation = validateImageFile(fileBytes, originalFileName)
            if (!validation.first) {
                println("Validation failed: ${validation.second}")
                return FileUploadResponse(
                    success = false,
                    message = validation.second
                )
            }
            println("File validation passed")

            // Generate unique filename
            val fileExtension = getFileExtension(originalFileName)
            val uniqueFileName = "profile_${userId}_${System.currentTimeMillis()}.$fileExtension"
            val dropboxPath = "${dropboxConfig.basePath}/$PROFILE_PICS_FOLDER/$uniqueFileName"
            println("Generated dropbox path: $dropboxPath")

            // Upload to Dropbox
            println("Starting Dropbox upload...")
            val uploadedFile = uploadToDropbox(fileBytes, dropboxPath, originalFileName)
            println("Dropbox upload completed successfully")

            val response = FileUploadResponse(
                success = true,
                fileUrl = uploadedFile.publicUrl,
                fileName = uploadedFile.fileName,
                fileSize = uploadedFile.fileSize,
                message = "Profile picture uploaded successfully"
            )
            println("Upload response: $response")
            println("===================")

            response

        } catch (e: Exception) {
            println("ERROR in uploadProfilePicture: ${e.message}")
            e.printStackTrace()
            FileUploadResponse(
                success = false,
                message = "Upload failed: ${e.message ?: "Unknown error"}"
            )
        }
    }

    /**
     * Upload document/file
     */
    suspend fun uploadDocument(
        inputStream: InputStream,
        originalFileName: String,
        userId: String,
        category: String = "general"
    ): FileUploadResponse {
        return try {
            val fileBytes = inputStream.readBytes()

            // Validate file size
            if (fileBytes.size > MAX_FILE_SIZE) {
                return FileUploadResponse(
                    success = false,
                    message = "File size exceeds maximum limit of ${MAX_FILE_SIZE / (1024 * 1024)}MB"
                )
            }

            // Generate unique filename
            val fileExtension = getFileExtension(originalFileName)
            val sanitizedCategory = category.replace(Regex("[^a-zA-Z0-9]"), "_")
            val uniqueFileName = "${sanitizedCategory}_${userId}_${System.currentTimeMillis()}.$fileExtension"
            val dropboxPath = "${dropboxConfig.basePath}/$DOCUMENTS_FOLDER/$sanitizedCategory/$uniqueFileName"

            // Upload to Dropbox
            val uploadedFile = uploadToDropbox(fileBytes, dropboxPath, originalFileName)

            FileUploadResponse(
                success = true,
                fileUrl = uploadedFile.publicUrl,
                fileName = uploadedFile.fileName,
                fileSize = uploadedFile.fileSize,
                message = "Document uploaded successfully"
            )

        } catch (e: Exception) {
            println("ERROR in uploadDocument: ${e.message}")
            e.printStackTrace()
            FileUploadResponse(
                success = false,
                message = "Upload failed: ${e.message ?: "Unknown error"}"
            )
        }
    }

    /**
     * Delete file from Dropbox
     */
    suspend fun deleteFile(filePath: String): FileDeleteResponse {
        return try {
            dbxClient.files().deleteV2(filePath)
            FileDeleteResponse(
                success = true,
                message = "File deleted successfully"
            )
        } catch (e: Exception) {
            FileDeleteResponse(
                success = false,
                message = "Delete failed: ${e.message}"
            )
        }
    }

    /**
     * Generate shareable link for existing file
     */
    suspend fun getShareableLink(filePath: String): String? {
        return try {
            val sharedLink = dbxClient.sharing().createSharedLinkWithSettings(filePath)
            convertToDirectLink(sharedLink.url)
        } catch (e: Exception) {
            null
        }
    }

    private fun uploadToDropbox(
        fileBytes: ByteArray,
        dropboxPath: String,
        originalFileName: String
    ): UploadedFile {
        try {
            println("=== DROPBOX UPLOAD DEBUG ===")
            println("Uploading to path: $dropboxPath")
            println("File size: ${fileBytes.size} bytes")

            // Upload file
            val metadata = dbxClient.files().uploadBuilder(dropboxPath)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(fileBytes.inputStream())

            println("Upload successful. Metadata: name=${metadata.name}, size=${metadata.size}")

            // Create shared link
            println("Creating shared link...")
            val sharedLink = try {
                val link = dbxClient.sharing().createSharedLinkWithSettings(dropboxPath)
                println("Shared link created successfully: ${link.url}")
                link
            } catch (e: Exception) {
                println("Failed to create shared link, trying to get existing one: ${e.message}")
                // If shared link creation fails, try to get existing one
                try {
                    val existingLinks = dbxClient.sharing().listSharedLinksBuilder()
                        .withPath(dropboxPath)
                        .start()
                    val existingLink = existingLinks.links.firstOrNull()
                    if (existingLink != null) {
                        println("Found existing shared link: ${existingLink.url}")
                        existingLink
                    } else {
                        println("No existing shared link found")
                        throw Exception("Failed to create or retrieve shared link: ${e.message}")
                    }
                } catch (ex: Exception) {
                    println("Failed to get existing shared link: ${ex.message}")
                    throw Exception("Failed to create or retrieve shared link: ${ex.message}")
                }
            }

            val publicUrl = convertToDirectLink(sharedLink?.url ?: "")
            val mimeType = tika.detect(fileBytes)

            println("Public URL: $publicUrl")
            println("MIME type: $mimeType")
            println("============================")

            return UploadedFile(
                fileName = metadata.name,
                originalName = originalFileName,
                fileSize = metadata.size,
                mimeType = mimeType,
                dropboxPath = dropboxPath,
                publicUrl = publicUrl
            )
        } catch (e: Exception) {
            println("ERROR in uploadToDropbox: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    private fun validateImageFile(fileBytes: ByteArray, fileName: String): Pair<Boolean, String> {
        println("=== FILE VALIDATION DEBUG ===")
        println("File name: $fileName")
        println("File size: ${fileBytes.size} bytes")

        // Check file size
        if (fileBytes.size > MAX_FILE_SIZE) {
            return false to "File size exceeds maximum limit of ${MAX_FILE_SIZE / (1024 * 1024)}MB"
        }

        // Debug file extension
        val extension = getFileExtension(fileName).lowercase()
        println("File extension: '$extension'")

        // Debug MIME type detection
        val mimeType = tika.detect(fileBytes)
        val mimeTypeWithFilename = try {
            tika.detect(fileBytes, fileName)
        } catch (e: Exception) {
            "error detecting with filename"
        }

        println("MIME type (content only): '$mimeType'")
        println("MIME type (with filename): '$mimeTypeWithFilename'")

        // Debug first few bytes
        val hexBytes = fileBytes.take(16).joinToString(" ") {
            "%02X".format(it.toInt() and 0xFF)
        }
        println("First 16 bytes (hex): $hexBytes")

        println("Allowed image types: $ALLOWED_IMAGE_TYPES")
        println("==============================")

        // Check file extension
        val allowedExtensions = setOf("jpg", "jpeg", "png", "gif", "webp")
        if (extension !in allowedExtensions) {
            return false to "Invalid file extension '$extension'. Only JPG, PNG, GIF, and WebP files are allowed"
        }

        // Check MIME type
        val isValidMimeType = mimeType in ALLOWED_IMAGE_TYPES ||
                mimeTypeWithFilename in ALLOWED_IMAGE_TYPES

        if (!isValidMimeType) {
            return false to "Invalid file type. Content MIME: '$mimeType', Filename MIME: '$mimeTypeWithFilename'. Only image files are allowed"
        }

        return true to "Valid image file"
    }

    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    private fun convertToDirectLink(dropboxUrl: String): String {
        // Convert Dropbox sharing URL to direct download URL
        return if (dropboxUrl.contains("dropbox.com")) {
            dropboxUrl.replace("?dl=0", "?raw=1")
        } else {
            dropboxUrl
        }
    }

    // Add this test function to your FileService class
    suspend fun testDropboxConnection(): String {
        return try {
            println("=== TESTING DROPBOX CONNECTION ===")
            println("Access token (first 10 chars): ${dropboxConfig.accessToken.take(10)}...")

            // Test basic API call
            val account = dbxClient.users().getCurrentAccount()
            println("Connected successfully!")
            println("Account name: ${account.name.displayName}")
            println("Account email: ${account.email}")

            // Test folder creation
            val testFolderPath = "${dropboxConfig.basePath}/test-folder"
            try {
                dbxClient.files().createFolderV2(testFolderPath)
                println("Test folder created: $testFolderPath")
            } catch (e: Exception) {
                println("Folder creation test: ${e.message}")
            }

            println("==================================")
            "Connection successful"
        } catch (e: Exception) {
            println("ERROR testing Dropbox connection: ${e.message}")
            e.printStackTrace()
            "Connection failed: ${e.message}"
        }
    }
}