package services

import com.example.models.FileUploadResponse
import com.example.models.FileDeleteResponse
import com.example.repositories.FileRecord
import com.example.repositories.FileRepository
import org.apache.tika.Tika
import services.storage.FileStorage
import services.storage.FileStorageException
import java.io.InputStream
import java.util.*

/**
 * S3-based FileService using the FileStorage abstraction
 * Enforces folder structure: tenantId/module/type/filename
 * Stores only object keys in database, generates signed URLs on demand
 */
class S3FileService(
    private val fileStorage: FileStorage,
    private val fileRepository: FileRepository
) {
    private val tika = Tika()

    companion object {
        private val ALLOWED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
        )
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
    }

    /**
     * Upload a file with enforced folder structure
     * @param tenantId Tenant ID (e.g., school ID, organization ID)
     * @param module Module name (e.g., "profile", "documents", "assignments")
     * @param type File type category (e.g., "image", "pdf", "video")
     * @param inputStream File content
     * @param originalFileName Original filename
     * @param uploadedBy User ID who uploaded the file
     * @param validateAsImage If true, validates that the file is an image
     */
    suspend fun uploadFile(
        tenantId: String,
        module: String,
        type: String,
        inputStream: InputStream,
        originalFileName: String,
        uploadedBy: String,
        validateAsImage: Boolean = false
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

            // Validate image if required
            if (validateAsImage) {
                val validation = validateImageFile(fileBytes, originalFileName)
                if (!validation.first) {
                    return FileUploadResponse(
                        success = false,
                        message = validation.second
                    )
                }
            }

            // Generate unique filename and object key with enforced structure
            val fileExtension = getFileExtension(originalFileName)
            val uniqueFileName = "${module}_${uploadedBy}_${System.currentTimeMillis()}.$fileExtension"
            val objectKey = buildObjectKey(tenantId, module, type, uniqueFileName)

            // Detect MIME type
            val mimeType = tika.detect(fileBytes, originalFileName)

            // Upload to storage
            fileStorage.uploadFile(
                objectKey = objectKey,
                inputStream = fileBytes.inputStream(),
                contentType = mimeType,
                contentLength = fileBytes.size.toLong()
            )

            // Save metadata to database (no tenantId - schema isolation)
            fileRepository.create(
                module = module,
                type = type,
                objectKey = objectKey,
                originalFileName = originalFileName,
                fileSize = fileBytes.size.toLong(),
                mimeType = mimeType,
                uploadedBy = uploadedBy
            )

            // Generate signed URL for immediate access
            val signedUrl = fileStorage.generateSignedUrl(objectKey, expirationMinutes = 60)

            FileUploadResponse(
                success = true,
                fileUrl = signedUrl,
                fileName = uniqueFileName,
                fileSize = fileBytes.size.toLong(),
                objectKey = objectKey,
                message = "File uploaded successfully"
            )

        } catch (e: FileStorageException) {
            FileUploadResponse(
                success = false,
                message = "Upload failed: ${e.message ?: "Unknown error"}"
            )
        } catch (e: Exception) {
            FileUploadResponse(
                success = false,
                message = "Upload failed: ${e.message ?: "Unknown error"}"
            )
        }
    }

    /**
     * Upload profile picture (convenience method)
     * Ensures one user has only one profile picture by deleting the old one
     */
    suspend fun uploadProfilePicture(
        tenantId: String,
        inputStream: InputStream,
        originalFileName: String,
        userId: String
    ): FileUploadResponse {
        // Delete existing profile picture for this user
        val userUuid = try {
            UUID.fromString(userId)
        } catch (e: Exception) {
            return FileUploadResponse(
                success = false,
                message = "Invalid user ID format"
            )
        }

        val existingProfilePics = fileRepository.findByModuleType("profile", "image")
            .filter { it.uploadedBy == userUuid }

        existingProfilePics.forEach { existingFile ->
            try {
                fileStorage.deleteFile(existingFile.objectKey)
                fileRepository.softDeleteByObjectKey(existingFile.objectKey)
            } catch (e: Exception) {
                // Silently continue if deletion fails
            }
        }

        return uploadFile(
            tenantId = tenantId,
            module = "profile",
            type = "image",
            inputStream = inputStream,
            originalFileName = originalFileName,
            uploadedBy = userId,
            validateAsImage = true
        )
    }

    /**
     * Upload profile picture with bytes (for compatibility)
     */
    suspend fun uploadProfilePicture(
        tenantId: String,
        userId: String,
        fileName: String,
        bytes: ByteArray
    ): FileUploadResponse {

        return uploadProfilePicture(
            tenantId = tenantId,
            inputStream = bytes.inputStream(),
            originalFileName = fileName,
            userId = userId
        )
    }

    /**
     * Upload post image (convenience method)
     */
    suspend fun uploadPostImage(
        tenantId: String,
        inputStream: InputStream,
        originalFileName: String,
        postId: String,
        userId: String
    ): FileUploadResponse {
        return uploadFile(
            tenantId = tenantId,
            module = "posts",
            type = "image",
            inputStream = inputStream,
            originalFileName = originalFileName,
            uploadedBy = userId,
            validateAsImage = true
        )
    }

    /**
     * Upload post image with bytes (for compatibility)
     */
    suspend fun uploadPostImage(
        tenantId: String,
        postId: String,
        userId: String,
        fileName: String,
        bytes: ByteArray
    ): FileUploadResponse {
        return uploadPostImage(
            tenantId = tenantId,
            inputStream = bytes.inputStream(),
            originalFileName = fileName,
            postId = postId,
            userId = userId
        )
    }

    /**
     * Upload document (convenience method)
     */
    suspend fun uploadDocument(
        tenantId: String,
        category: String,
        inputStream: InputStream,
        originalFileName: String,
        userId: String
    ): FileUploadResponse {
        val fileExtension = getFileExtension(originalFileName).lowercase()
        val type = when (fileExtension) {
            "pdf" -> "pdf"
            "doc", "docx" -> "document"
            "xls", "xlsx" -> "spreadsheet"
            "jpg", "jpeg", "png", "gif", "webp" -> "image"
            "mp4", "mov", "avi" -> "video"
            else -> "other"
        }

        return uploadFile(
            tenantId = tenantId,
            module = "documents",
            type = category,
            inputStream = inputStream,
            originalFileName = originalFileName,
            uploadedBy = userId,
            validateAsImage = false
        )
    }

    /**
     * Upload document with bytes (for compatibility)
     */
    suspend fun uploadDocument(
        tenantId: String,
        userId: String,
        category: String,
        originalName: String,
        bytes: ByteArray
    ): FileUploadResponse {

        return uploadDocument(
            tenantId = tenantId,
            category = category,
            inputStream = bytes.inputStream(),
            originalFileName = originalName,
            userId = userId
        )
    }

    /**
     * Delete file by object key
     */
    suspend fun deleteFile(objectKey: String): FileDeleteResponse {
        return try {
            // Soft delete in database first
            fileRepository.softDeleteByObjectKey(objectKey)

            // Delete from storage
            fileStorage.deleteFile(objectKey)

            FileDeleteResponse(
                success = true,
                message = "File deleted successfully"
            )
        } catch (e: FileStorageException) {
            FileDeleteResponse(
                success = false,
                message = "Delete failed: ${e.message}"
            )
        } catch (e: Exception) {
            FileDeleteResponse(
                success = false,
                message = "Delete failed: ${e.message}"
            )
        }
    }

    /**
     * Delete file by ID
     */
    suspend fun deleteFileById(fileId: UUID): FileDeleteResponse {
        return try {
            val fileRecord = fileRepository.findById(fileId)
                ?: return FileDeleteResponse(
                    success = false,
                    message = "File not found"
                )

            deleteFile(fileRecord.objectKey)
        } catch (e: Exception) {
            FileDeleteResponse(
                success = false,
                message = "Delete failed: ${e.message}"
            )
        }
    }

    /**
     * Generate a signed URL for a file (for temporary access)
     */
    suspend fun generateSignedUrl(fileId: UUID, expirationMinutes: Long = 60): String? {
        return try {
            val fileRecord = fileRepository.findById(fileId)
                ?: return null

            fileStorage.generateSignedUrl(fileRecord.objectKey, expirationMinutes)
        } catch (e: Exception) {
            println("ERROR generating signed URL: ${e.message}")
            null
        }
    }

    /**
     * Generate a signed URL by object key
     */
    suspend fun generateSignedUrlByKey(objectKey: String, expirationMinutes: Long = 60): String? {
        return try {
            fileStorage.generateSignedUrl(objectKey, expirationMinutes)
        } catch (e: Exception) {
            println("ERROR generating signed URL: ${e.message}")
            null
        }
    }

    /**
     * Generate a public URL by file ID (no signing, no expiration)
     * Use this for public content like profile pictures, logos, post images
     * Requires bucket to be configured as public
     */
    suspend fun generatePublicUrl(fileId: String): String? {
        return try {
            val fileRecord = fileRepository.findById(UUID.fromString(fileId)) ?: return null
            fileStorage.generatePublicUrl(fileRecord.objectKey)
        } catch (e: Exception) {
            println("ERROR generating public URL: ${e.message}")
            null
        }
    }

    /**
     * Generate a public URL by object key (no signing, no expiration)
     * Use this for public content like profile pictures, logos, post images
     * Requires bucket to be configured as public
     */
    fun generatePublicUrlByKey(objectKey: String): String {
        return fileStorage.generatePublicUrl(objectKey)
    }

    /**
     * Get file metadata from database
     */
    suspend fun getFileById(fileId: UUID): FileRecord? {
        return fileRepository.findById(fileId)
    }

    /**
     * Get file metadata by object key
     */
    suspend fun getFileByObjectKey(objectKey: String): FileRecord? {
        return fileRepository.findByObjectKey(objectKey)
    }

    /**
     * Get all files uploaded by a user
     */
    suspend fun getFilesByUser(userId: UUID): List<FileRecord> {
        return fileRepository.findByUploadedBy(userId)
    }

    /**
     * Get files by module and type for current tenant schema
     */
    suspend fun getFilesByModuleType(module: String, type: String): List<FileRecord> {
        return fileRepository.findByModuleType(module, type)
    }

    /**
     * Get total storage used by current tenant (schema-isolated)
     */
    suspend fun getTotalStorageByTenant(): Long {
        return fileRepository.getTotalStorageByTenant()
    }

    /**
     * Get total storage used by user
     */
    suspend fun getTotalStorageByUser(userId: UUID): Long {
        return fileRepository.getTotalStorageByUser(userId)
    }

    /**
     * Build object key with enforced folder structure
     * Format: tenantId/module/type/filename
     */
    private fun buildObjectKey(tenantId: String, module: String, type: String, filename: String): String {
        val sanitizedTenantId = sanitizePath(tenantId)
        val sanitizedModule = sanitizePath(module)
        val sanitizedType = sanitizePath(type)
        val sanitizedFilename = sanitizePath(filename)

        return "$sanitizedTenantId/$sanitizedModule/$sanitizedType/$sanitizedFilename"
    }

    /**
     * Sanitize path components to prevent path traversal attacks
     */
    private fun sanitizePath(path: String): String {
        return path.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    /**
     * Validate image file
     */
    private fun validateImageFile(fileBytes: ByteArray, fileName: String): Pair<Boolean, String> {
        // Check file size
        if (fileBytes.size > MAX_FILE_SIZE) {
            return false to "File size exceeds maximum limit of ${MAX_FILE_SIZE / (1024 * 1024)}MB"
        }

        // Check file extension
        val extension = getFileExtension(fileName).lowercase()
        val allowedExtensions = setOf("jpg", "jpeg", "png", "gif", "webp")
        if (extension !in allowedExtensions) {
            return false to "Invalid file extension '$extension'. Only JPG, PNG, GIF, and WebP files are allowed"
        }

        // Check MIME type
        val mimeType = tika.detect(fileBytes)
        val mimeTypeWithFilename = try {
            tika.detect(fileBytes, fileName)
        } catch (e: Exception) {
            "error detecting with filename"
        }

        val isValidMimeType = mimeType in ALLOWED_IMAGE_TYPES ||
                mimeTypeWithFilename in ALLOWED_IMAGE_TYPES

        if (!isValidMimeType) {
            return false to "Invalid file type. Content MIME: '$mimeType', Filename MIME: '$mimeTypeWithFilename'. Only image files are allowed"
        }

        return true to "Valid image file"
    }

    /**
     * Get file extension from filename
     */
    private fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    /**
     * Test storage connection
     */
    suspend fun testStorageConnection(): String {
        return try {
            // Upload a small test file
            val testContent = "Test file content - ${System.currentTimeMillis()}"
            val testObjectKey = "test/connection/test.txt"

            fileStorage.uploadFile(
                objectKey = testObjectKey,
                inputStream = testContent.byteInputStream(),
                contentType = "text/plain",
                contentLength = testContent.length.toLong()
            )

            // Check if file exists
            val exists = fileStorage.fileExists(testObjectKey)

            // Generate signed URL
            fileStorage.generateSignedUrl(testObjectKey, expirationMinutes = 5)

            // Clean up test file
            fileStorage.deleteFile(testObjectKey)

            "Connection successful - File upload, exists check, URL generation, and deletion all working"
        } catch (e: Exception) {
            "Connection failed: ${e.message}"
        }
    }
}
