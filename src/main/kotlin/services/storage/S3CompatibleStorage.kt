package services.storage

import config.S3StorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.io.InputStream
import java.net.URI
import java.time.Duration

/**
 * S3-compatible storage implementation that works with:
 * - AWS S3
 * - Backblaze B2
 * - Cloudflare R2
 * - Any other S3-compatible service
 */
class S3CompatibleStorage(private val config: S3StorageConfig) : FileStorage {

    private val s3Client: S3Client by lazy {
        val credentials = AwsBasicCredentials.create(
            config.accessKeyId,
            config.secretAccessKey
        )

        val builder = S3Client.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(config.region))

        // Set custom endpoint for Backblaze B2 and Cloudflare R2
        if (config.endpoint != null) {
            builder.endpointOverride(URI.create(config.endpoint))
        }

        builder.build()
    }

    private val s3Presigner: S3Presigner by lazy {
        val credentials = AwsBasicCredentials.create(
            config.accessKeyId,
            config.secretAccessKey
        )

        val builder = S3Presigner.builder()
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .region(Region.of(config.region))

        // Set custom endpoint for presigned URLs
        if (config.endpoint != null) {
            builder.endpointOverride(URI.create(config.endpoint))
        }

        builder.build()
    }

    override suspend fun uploadFile(
        objectKey: String,
        inputStream: InputStream,
        contentType: String,
        contentLength: Long
    ): String = withContext(Dispatchers.IO) {
        try {
            val putObjectRequest = PutObjectRequest.builder()
                .bucket(config.bucketName)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build()

            val requestBody = RequestBody.fromInputStream(inputStream, contentLength)

            s3Client.putObject(putObjectRequest, requestBody)

            objectKey
        } catch (e: Exception) {
            throw FileStorageException("Failed to upload file: ${e.message}", e)
        }
    }

    override suspend fun deleteFile(objectKey: String) = withContext(Dispatchers.IO) {
        try {
            val deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(config.bucketName)
                .key(objectKey)
                .build()

            s3Client.deleteObject(deleteObjectRequest)
        } catch (e: Exception) {
            throw FileStorageException("Failed to delete file: ${e.message}", e)
        }
    }

    override suspend fun generateSignedUrl(objectKey: String, expirationMinutes: Long): String =
        withContext(Dispatchers.IO) {
            try {
                val getObjectRequest = GetObjectRequest.builder()
                    .bucket(config.bucketName)
                    .key(objectKey)
                    .build()

                val presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes))
                    .getObjectRequest(getObjectRequest)
                    .build()

                val presignedRequest = s3Presigner.presignGetObject(presignRequest)

                presignedRequest.url().toString()
            } catch (e: Exception) {
                throw FileStorageException("Failed to generate signed URL: ${e.message}", e)
            }
        }

    override suspend fun fileExists(objectKey: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val headObjectRequest = HeadObjectRequest.builder()
                .bucket(config.bucketName)
                .key(objectKey)
                .build()

            s3Client.headObject(headObjectRequest)
            true
        } catch (e: NoSuchKeyException) {
            false
        } catch (e: Exception) {
            throw FileStorageException("Failed to check file existence: ${e.message}", e)
        }
    }

    override suspend fun getFileMetadata(objectKey: String): Map<String, String> =
        withContext(Dispatchers.IO) {
            try {
                val headObjectRequest = HeadObjectRequest.builder()
                    .bucket(config.bucketName)
                    .key(objectKey)
                    .build()

                val response = s3Client.headObject(headObjectRequest)

                mapOf(
                    "contentType" to (response.contentType() ?: "application/octet-stream"),
                    "contentLength" to response.contentLength().toString(),
                    "lastModified" to (response.lastModified()?.toString() ?: ""),
                    "etag" to (response.eTag() ?: "")
                )
            } catch (e: Exception) {
                throw FileStorageException("Failed to get file metadata: ${e.message}", e)
            }
        }

    /**
     * Test the connection to the storage provider
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val listBucketsRequest = ListBucketsRequest.builder().build()
            s3Client.listBuckets(listBucketsRequest)
            true
        } catch (e: Exception) {
            println("Storage connection test failed: ${e.message}")
            false
        }
    }

    /**
     * Close the S3 client and presigner
     */
    fun close() {
        s3Client.close()
        s3Presigner.close()
    }
}
