package com.example.services

import com.example.config.SupabaseConfig
import com.example.models.FileUploadResponse
import com.example.models.FileDeleteResponse
import com.example.models.UploadedFile
import org.apache.tika.Tika
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.endpoints.Endpoint
import software.amazon.awssdk.services.s3.S3Configuration
import java.io.InputStream
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileService(private val supabaseConfig: SupabaseConfig) {

    private val s3Client: S3Client
    private val tika = Tika()

    init {
        val credentials = AwsBasicCredentials.create(
            supabaseConfig.accessKeyId,
            supabaseConfig.secretAccessKey
        )

        s3Client = S3Client.builder()
            .region(Region.of(supabaseConfig.region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .endpointOverride(URI.create(supabaseConfig.endpoint))
            .serviceConfiguration(
                S3Configuration.builder()
                    .pathStyleAccessEnabled(true) // Important for Supabase
                    .build()
            )
            .build()
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
            val s3Key = "$PROFILE_PICS_FOLDER/$uniqueFileName"
            println("Generated S3 key: $s3Key")

            // Upload to S3/Supabase
            println("Starting S3 upload...")
            val uploadedFile = uploadToS3(fileBytes, s3Key, originalFileName)
            println("S3 upload completed successfully")

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
            val s3Key = "$DOCUMENTS_FOLDER/$sanitizedCategory/$uniqueFileName"

            // Upload to S3/Supabase
            val uploadedFile = uploadToS3(fileBytes, s3Key, originalFileName)

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
     * Delete file from S3/Supabase
     */
    suspend fun deleteFile(s3Key: String): FileDeleteResponse {
        return try {
            withContext(Dispatchers.IO) {
                val deleteRequest = DeleteObjectRequest.builder()
                    .bucket(supabaseConfig.bucket)
                    .key(s3Key)
                    .build()

                s3Client.deleteObject(deleteRequest)

                FileDeleteResponse(
                    success = true,
                    message = "File deleted successfully"
                )
            }
        } catch (e: Exception) {
            FileDeleteResponse(
                success = false,
                message = "Delete failed: ${e.message}"
            )
        }
    }

    /**
     * Generate public URL for file
     */
    suspend fun getPublicUrl(s3Key: String): String {
        // Supabase public URL format
        val projectId = supabaseConfig.endpoint.substringAfter("https://").substringBefore(".supabase.co")
        return "https://${projectId}.supabase.co/storage/v1/object/public/${supabaseConfig.bucket}/$s3Key"
    }

    /**
     * Generate shareable link for existing file
     */
    suspend fun getShareableLink(s3Key: String): String? {
        return try {
            getPublicUrl(s3Key)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun uploadToS3(
        fileBytes: ByteArray,
        s3Key: String,
        originalFileName: String
    ): UploadedFile = withContext(Dispatchers.IO) {
        try {
            println("=== S3 UPLOAD DEBUG ===")
            println("Uploading to S3 key: $s3Key")
            println("File size: ${fileBytes.size} bytes")
            println("Bucket: ${supabaseConfig.bucket}")
            println("Endpoint: ${supabaseConfig.endpoint}")

            val mimeType = tika.detect(fileBytes, originalFileName)
            println("Detected MIME type: $mimeType")

            val putObjectRequest = PutObjectRequest.builder()
                .bucket(supabaseConfig.bucket)
                .key(s3Key)
                .contentType(mimeType)
                .contentLength(fileBytes.size.toLong())
                .build()

            val requestBody = RequestBody.fromBytes(fileBytes)

            val response = s3Client.putObject(putObjectRequest, requestBody)
            println("S3 Upload successful. ETag: ${response.eTag()}")

            // Generate public URL
            val publicUrl = getPublicUrl(s3Key)
            println("Public URL: $publicUrl")
            println("=======================")

            return@withContext UploadedFile(
                fileName = s3Key.substringAfterLast('/'),
                originalName = originalFileName,
                fileSize = fileBytes.size.toLong(),
                mimeType = mimeType,
                dropboxPath = s3Key, // Using same field name for compatibility
                publicUrl = publicUrl
            )
        } catch (e: Exception) {
            println("ERROR in uploadToS3: ${e.message}")
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

    // Test S3/Supabase connection
    suspend fun testSupabaseConnection(): String {
        return try {
            withContext(Dispatchers.IO) {
                println("=== TESTING S3/SUPABASE CONNECTION ===")
                println("Bucket: ${supabaseConfig.bucket}")
                println("Endpoint: ${supabaseConfig.endpoint}")
                println("Access Key ID: ${supabaseConfig.accessKeyId.take(10)}...")

                // Test bucket access
                val headBucketRequest = HeadBucketRequest.builder()
                    .bucket(supabaseConfig.bucket)
                    .build()

                s3Client.headBucket(headBucketRequest)
                println("Bucket access successful!")

                // Test list objects (optional)
                try {
                    val listRequest = ListObjectsV2Request.builder()
                        .bucket(supabaseConfig.bucket)
                        .maxKeys(1)
                        .build()

                    val listResponse = s3Client.listObjectsV2(listRequest)
                    println("List objects successful. Found ${listResponse.keyCount()} objects")
                } catch (e: Exception) {
                    println("List objects test: ${e.message}")
                }

                println("=======================================")
                "Connection successful"
            }
        } catch (e: Exception) {
            println("ERROR testing S3/Supabase connection: ${e.message}")
            e.printStackTrace()
            "Connection failed: ${e.message}"
        }
    }

    // List files in bucket
    suspend fun listFiles(prefix: String = "", maxKeys: Int = 100): List<String> {
        return try {
            withContext(Dispatchers.IO) {
                val listRequest = ListObjectsV2Request.builder()
                    .bucket(supabaseConfig.bucket)
                    .prefix(prefix)
                    .maxKeys(maxKeys)
                    .build()

                val response = s3Client.listObjectsV2(listRequest)
                response.contents().map { it.key() }
            }
        } catch (e: Exception) {
            println("Error listing files: ${e.message}")
            emptyList()
        }
    }

    // Close S3 client when done
    fun close() {
        s3Client.close()
    }
}