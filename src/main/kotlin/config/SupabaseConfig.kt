package com.example.config

data class SupabaseConfig(
    val accessKeyId: String,
    val secretAccessKey: String,
    val region: String,
    val bucket: String,
    val endpoint: String // Supabase S3-compatible endpoint
) {
    companion object {
        fun fromEnvironment(): SupabaseConfig {
            return SupabaseConfig(
                accessKeyId = System.getenv("SUPABASE_ACCESS_KEY_ID") ?: throw RuntimeException("SUPABASE_ACCESS_KEY_ID not found"),
                secretAccessKey = System.getenv("SUPABASE_SECRET_ACCESS_KEY") ?: throw RuntimeException("SUPABASE_SECRET_ACCESS_KEY not found"),
                region = System.getenv("SUPABASE_REGION") ?: "us-east-1",
                bucket = System.getenv("SUPABASE_BUCKET") ?: "sms",
                endpoint = System.getenv("SUPABASE_ENDPOINT") ?: throw RuntimeException("SUPABASE_ENDPOINT not found")
            )
        }

        // For your specific setup
        fun forSupabase(projectUrl: String, accessKeyId: String, secretAccessKey: String, bucket: String): SupabaseConfig {
            // Extract project ID from URL: https://tzieatliufhksqtxbyoz.supabase.co
            val projectId = projectUrl.substringAfter("https://").substringBefore(".supabase.co")
            val endpoint = "https://$projectId.supabase.co/storage/v1/s3"

            return SupabaseConfig(
                accessKeyId = accessKeyId,
                secretAccessKey = secretAccessKey,
                region = "us-east-1", // Supabase uses us-east-1
                bucket = bucket,
                endpoint = endpoint
            )
        }
    }
}