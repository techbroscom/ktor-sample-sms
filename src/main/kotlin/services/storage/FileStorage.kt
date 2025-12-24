package services.storage

import java.io.InputStream

/**
 * FileStorage abstraction for S3-compatible storage providers.
 * Supports AWS S3, Backblaze B2, Cloudflare R2, and other S3-compatible services.
 */
interface FileStorage {
    /**
     * Upload a file to storage
     * @param objectKey The S3 object key (path) for the file
     * @param inputStream The file content as InputStream
     * @param contentType MIME type of the file
     * @param contentLength Size of the file in bytes
     * @return The object key of the uploaded file
     */
    suspend fun uploadFile(
        objectKey: String,
        inputStream: InputStream,
        contentType: String,
        contentLength: Long
    ): String

    /**
     * Delete a file from storage
     * @param objectKey The S3 object key of the file to delete
     */
    suspend fun deleteFile(objectKey: String)

    /**
     * Generate a signed URL for temporary access to a file
     * @param objectKey The S3 object key
     * @param expirationMinutes How long the URL should be valid (default: 60 minutes)
     * @return Signed URL string
     */
    suspend fun generateSignedUrl(objectKey: String, expirationMinutes: Long = 60): String

    /**
     * Generate a public URL for a file (no signing, no expiration)
     * Use this for public content like profile pictures, logos, post images
     * Requires the bucket to be configured as public
     * @param objectKey The S3 object key
     * @return Public URL string
     */
    fun generatePublicUrl(objectKey: String): String

    /**
     * Check if a file exists in storage
     * @param objectKey The S3 object key
     * @return true if file exists, false otherwise
     */
    suspend fun fileExists(objectKey: String): Boolean

    /**
     * Get file metadata (size, content-type, etc.)
     * @param objectKey The S3 object key
     * @return Map of metadata
     */
    suspend fun getFileMetadata(objectKey: String): Map<String, String>
}

/**
 * Data class for file upload results
 */
data class FileUploadResult(
    val objectKey: String,
    val fileSize: Long,
    val contentType: String
)

/**
 * Exception thrown when file storage operations fail
 */
class FileStorageException(message: String, cause: Throwable? = null) : Exception(message, cause)
