package com.example.jobs

import com.example.repositories.FileRepository
import com.example.repositories.UserRepository
import com.example.repositories.SchoolConfigRepository
import com.example.repositories.PostImageRepository
import com.example.services.TenantService
import com.example.tenant.TenantContext
import com.example.tenant.TenantContextHolder
import io.ktor.server.application.*
import kotlinx.coroutines.*
import services.storage.S3CompatibleStorage
import kotlin.time.Duration.Companion.hours
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * S3 Cleanup Job
 *
 * This job performs two types of cleanup:
 * 1. Orphan image cleanup: Removes files from S3 that are not referenced in any database table
 * 2. Soft-deleted file cleanup: Removes files from S3 and database that have been soft-deleted for more than X days
 *
 * Schedule: Runs every 24 hours
 */
class S3CleanupJob(
    private val fileRepository: FileRepository,
    private val userRepository: UserRepository,
    private val schoolConfigRepository: SchoolConfigRepository,
    private val postImageRepository: PostImageRepository,
    private val tenantService: TenantService,
    private val s3Storage: S3CompatibleStorage,
    private val application: Application
) {
    private val logger = LoggerFactory.getLogger(S3CleanupJob::class.java)
    private var job: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Configuration
    private val softDeletedFileAgeInDays = 7 // Delete soft-deleted files older than 7 days

    fun start() {
        job = coroutineScope.launch {
            while (isActive) {
                try {
                    logger.info("Starting S3 cleanup job")
                    cleanupS3ForAllTenants()
                    delay(24.hours)
                } catch (e: Exception) {
                    logger.error("S3 Cleanup Job failed: ${e.message}", e)
                    delay(1.hours) // Retry after 1 hour on error
                }
            }
        }
    }

    private suspend fun cleanupS3ForAllTenants() {
        val tenants = tenantService.getAllTenants()
        logger.info("Starting S3 cleanup for ${tenants.size} tenants")

        var successCount = 0
        var failureCount = 0
        var totalOrphanedFilesDeleted = 0
        var totalSoftDeletedFilesDeleted = 0

        for (tenant in tenants) {
            try {
                logger.info("Processing S3 cleanup for tenant ${tenant.name} (${tenant.schemaName})")

                // Step 1: Cleanup orphaned files in S3
                val orphanedCount = cleanupOrphanedFilesForTenant(tenant)
                totalOrphanedFilesDeleted += orphanedCount

                // Step 2: Cleanup soft-deleted files
                val softDeletedCount = cleanupSoftDeletedFilesForTenant(tenant)
                totalSoftDeletedFilesDeleted += softDeletedCount

                logger.info(
                    "S3 Cleanup for tenant ${tenant.name} (${tenant.schemaName}): " +
                            "Deleted $orphanedCount orphaned files, $softDeletedCount soft-deleted files"
                )
                successCount++
            } catch (e: Exception) {
                logger.error("Failed to cleanup S3 for tenant ${tenant.name} (${tenant.schemaName})", e)
                failureCount++
            }
        }

        logger.info(
            "S3 Cleanup completed: $successCount succeeded, $failureCount failed, " +
                    "$totalOrphanedFilesDeleted orphaned files deleted, " +
                    "$totalSoftDeletedFilesDeleted soft-deleted files deleted"
        )
    }

    /**
     * Cleanup orphaned files in S3 for a specific tenant
     * Orphaned files are files that exist in S3 but are not referenced in any database table
     */
    private suspend fun cleanupOrphanedFilesForTenant(tenant: TenantContext): Int {
        try {
            // Get all files in S3 for this tenant (prefix with tenant ID)
            val s3Files = s3Storage.listFiles(prefix = "${tenant.id}/")

            if (s3Files.isEmpty()) {
                logger.debug("No S3 files found for tenant ${tenant.id}")
                return 0
            }

            logger.debug("Found ${s3Files.size} files in S3 for tenant ${tenant.id}")

            // Collect all S3 keys referenced in database tables with tenant context element
            val referencedKeys = withContext(TenantContextHolder.threadLocal.asContextElement(tenant)) {
                val keys = mutableSetOf<String>()

                // 1. Files table (active files only)
                keys.addAll(fileRepository.getAllActiveObjectKeys())

                // 2. Users table (profile pictures)
                keys.addAll(userRepository.getAllImageS3Keys())

                // 3. SchoolConfig table (school logos)
                keys.addAll(schoolConfigRepository.getAllLogoS3Keys())

                // 4. PostImages table (post images)
                keys.addAll(postImageRepository.getAllImageS3Keys())

                keys
            }

            logger.debug("Found ${referencedKeys.size} referenced S3 keys in database for tenant ${tenant.id}")

            // Find orphaned files (files in S3 not referenced in database)
            val orphanedFiles = s3Files.filter { it !in referencedKeys }

            if (orphanedFiles.isEmpty()) {
                logger.debug("No orphaned files found for tenant ${tenant.id}")
                return 0
            }

            logger.info("Found ${orphanedFiles.size} orphaned files in S3 for tenant ${tenant.id}")

            // Delete orphaned files from S3 (no tenant context needed)
            var deletedCount = 0
            for (orphanedFile in orphanedFiles) {
                try {
                    s3Storage.deleteFile(orphanedFile)
                    deletedCount++
                    logger.debug("Deleted orphaned file: $orphanedFile")
                } catch (e: Exception) {
                    logger.error("Failed to delete orphaned file $orphanedFile: ${e.message}", e)
                }
            }

            return deletedCount
        } catch (e: Exception) {
            logger.error("Error during orphaned file cleanup for tenant ${tenant.id}: ${e.message}", e)
            return 0
        }
    }

    /**
     * Cleanup soft-deleted files from S3 and database
     * Removes files that have been soft-deleted for more than the configured number of days
     */
    private suspend fun cleanupSoftDeletedFilesForTenant(tenant: TenantContext): Int {
        try {
            // Step 1: Get soft-deleted files with tenant context element
            val softDeletedFiles = withContext(TenantContextHolder.threadLocal.asContextElement(tenant)) {
                fileRepository.getSoftDeletedFiles(softDeletedFileAgeInDays)
            }

            if (softDeletedFiles.isEmpty()) {
                logger.debug("No soft-deleted files found older than $softDeletedFileAgeInDays days")
                return 0
            }

            logger.info("Found ${softDeletedFiles.size} soft-deleted files older than $softDeletedFileAgeInDays days")

            // Step 2: Delete files from S3 (no tenant context needed)
            var deletedCount = 0
            val successfullyDeletedIds = mutableListOf<UUID>()

            for (fileRecord in softDeletedFiles) {
                try {
                    s3Storage.deleteFile(fileRecord.objectKey)
                    logger.debug("Deleted soft-deleted file from S3: ${fileRecord.objectKey}")
                    successfullyDeletedIds.add(fileRecord.id)
                    deletedCount++
                } catch (e: Exception) {
                    logger.error("Failed to delete soft-deleted file from S3: ${fileRecord.objectKey}: ${e.message}", e)
                }
            }

            // Step 3: Hard delete from database (with tenant context element)
            if (successfullyDeletedIds.isNotEmpty()) {
                withContext(TenantContextHolder.threadLocal.asContextElement(tenant)) {
                    for (fileId in successfullyDeletedIds) {
                        try {
                            fileRepository.hardDelete(fileId)
                            logger.debug("Hard deleted file record from database: $fileId")
                        } catch (e: Exception) {
                            logger.error("Failed to delete file record from database: $fileId: ${e.message}", e)
                        }
                    }
                }
            }

            return deletedCount
        } catch (e: Exception) {
            logger.error("Error during soft-deleted file cleanup: ${e.message}", e)
            return 0
        }
    }

    fun stop() {
        job?.cancel()
        coroutineScope.cancel()
    }
}
