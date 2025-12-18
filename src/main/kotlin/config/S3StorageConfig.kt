package config

/**
 * Configuration for S3-compatible storage providers
 * Supports: AWS S3, Backblaze B2, Cloudflare R2
 */
data class S3StorageConfig(
    val provider: StorageProvider,
    val accessKeyId: String,
    val secretAccessKey: String,
    val region: String,
    val bucketName: String,
    val endpoint: String? = null // Required for Backblaze B2 and Cloudflare R2
) {
    companion object {
        /**
         * Create configuration from environment variables
         */
        fun fromEnvironment(): S3StorageConfig {
            val provider = System.getenv("STORAGE_PROVIDER")?.let {
                StorageProvider.valueOf(it.uppercase())
            } ?: StorageProvider.BACKBLAZE_B2

            val accessKeyId = System.getenv("STORAGE_ACCESS_KEY_ID")
                ?: throw IllegalStateException("STORAGE_ACCESS_KEY_ID environment variable is required")

            val secretAccessKey = System.getenv("STORAGE_SECRET_ACCESS_KEY")
                ?: throw IllegalStateException("STORAGE_SECRET_ACCESS_KEY environment variable is required")

            val region = System.getenv("STORAGE_REGION") ?: "us-west-002"
            val bucketName = System.getenv("STORAGE_BUCKET_NAME")
                ?: throw IllegalStateException("STORAGE_BUCKET_NAME environment variable is required")

            val endpoint = System.getenv("STORAGE_ENDPOINT")

            return S3StorageConfig(
                provider = provider,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                region = region,
                bucketName = bucketName,
                endpoint = endpoint ?: provider.getDefaultEndpoint(region)
            )
        }

        /**
         * Create Backblaze B2 configuration directly
         */
        fun forBackblazeB2(
            accessKeyId: String,
            secretAccessKey: String,
            region: String = "us-west-002",
            bucketName: String
        ): S3StorageConfig {
            return S3StorageConfig(
                provider = StorageProvider.BACKBLAZE_B2,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                region = region,
                bucketName = bucketName,
                endpoint = "https://s3.$region.backblazeb2.com"
            )
        }

        /**
         * Create Cloudflare R2 configuration
         */
        fun forCloudflareR2(
            accessKeyId: String,
            secretAccessKey: String,
            accountId: String,
            bucketName: String
        ): S3StorageConfig {
            return S3StorageConfig(
                provider = StorageProvider.CLOUDFLARE_R2,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                region = "auto",
                bucketName = bucketName,
                endpoint = "https://$accountId.r2.cloudflarestorage.com"
            )
        }

        /**
         * Create AWS S3 configuration
         */
        fun forAwsS3(
            accessKeyId: String,
            secretAccessKey: String,
            region: String,
            bucketName: String
        ): S3StorageConfig {
            return S3StorageConfig(
                provider = StorageProvider.AWS_S3,
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                region = region,
                bucketName = bucketName,
                endpoint = null // AWS S3 uses standard endpoints
            )
        }
    }
}

/**
 * Supported storage providers
 */
enum class StorageProvider {
    AWS_S3,
    BACKBLAZE_B2,
    CLOUDFLARE_R2;

    fun getDefaultEndpoint(region: String): String? {
        return when (this) {
            AWS_S3 -> null // AWS SDK handles endpoint automatically
            BACKBLAZE_B2 -> "https://s3.$region.backblazeb2.com"
            CLOUDFLARE_R2 -> null // Requires account ID, must be provided explicitly
        }
    }
}
