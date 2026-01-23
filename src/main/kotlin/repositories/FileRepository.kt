package com.example.repositories

import com.example.database.tables.Files
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class FileRepository {

    /**
     * Create a new file record within the tenant context
     * No tenantId needed - schema isolation handles multi-tenancy
     */
    suspend fun create(
        module: String,
        type: String,
        objectKey: String,
        originalFileName: String,
        fileSize: Long,
        mimeType: String,
        uploadedBy: String
    ): UUID = tenantDbQuery {
        val fileId = UUID.randomUUID()

        Files.insert {
            it[id] = fileId
            it[Files.module] = module
            it[Files.type] = type
            it[Files.objectKey] = objectKey
            it[Files.originalFileName] = originalFileName
            it[Files.fileSize] = fileSize
            it[Files.mimeType] = mimeType
            it[Files.uploadedBy] = UUID.fromString(uploadedBy)
            it[createdAt] = LocalDateTime.now()
        }

        fileId
    }

    /**
     * Find a file by ID
     */
    suspend fun findById(fileId: UUID): FileRecord? = tenantDbQuery {
        Files.selectAll()
            .where { (Files.id eq fileId) and (Files.deletedAt.isNull()) }
            .map { it.toFileRecord() }
            .singleOrNull()
    }

    /**
     * Find a file by object key
     */
    suspend fun findByObjectKey(objectKey: String): FileRecord? = tenantDbQuery {
        Files.selectAll()
            .where { (Files.objectKey eq objectKey) and (Files.deletedAt.isNull()) }
            .map { it.toFileRecord() }
            .singleOrNull()
    }

    /**
     * Find all files uploaded by a user
     */
    suspend fun findByUploadedBy(userId: UUID): List<FileRecord> = tenantDbQuery {
        Files.selectAll()
            .where { (Files.uploadedBy eq userId) and (Files.deletedAt.isNull()) }
            .map { it.toFileRecord() }
    }

    /**
     * Find files by module and type within current tenant schema
     * No tenantId needed - schema isolation provides multi-tenancy
     */
    suspend fun findByModuleType(module: String, type: String): List<FileRecord> = tenantDbQuery {
        Files.selectAll()
            .where {
                (Files.module eq module) and
                        (Files.type eq type) and
                        (Files.deletedAt.isNull())
            }
            .map { it.toFileRecord() }
    }

    /**
     * Soft delete a file by ID
     */
    suspend fun softDelete(fileId: UUID): Boolean = tenantDbQuery {
        Files.update({ Files.id eq fileId }) {
            it[deletedAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    /**
     * Soft delete a file by object key
     */
    suspend fun softDeleteByObjectKey(objectKey: String): Boolean = tenantDbQuery {
        Files.update({ Files.objectKey eq objectKey }) {
            it[deletedAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    /**
     * Hard delete a file by ID
     */
    suspend fun hardDelete(fileId: UUID): Boolean = tenantDbQuery {
        Files.deleteWhere { id eq fileId } > 0
    }

    /**
     * Update file metadata
     */
    suspend fun update(fileId: UUID, updates: Map<String, Any>): Boolean = tenantDbQuery {
        Files.update({ Files.id eq fileId }) { statement ->
            updates.forEach { (key, value) ->
                when (key) {
                    "originalFileName" -> statement[Files.originalFileName] = value as String
                    "fileSize" -> statement[Files.fileSize] = value as Long
                    "mimeType" -> statement[Files.mimeType] = value as String
                }
            }
            statement[updatedAt] = LocalDateTime.now()
        } > 0
    }

    /**
     * Get total storage used by current tenant
     * No tenantId parameter needed - uses current tenant's schema
     */
    suspend fun getTotalStorageByTenant(): Long = tenantDbQuery {
        Files.select(Files.fileSize.sum())
            .where { Files.deletedAt.isNull() }
            .map { it[Files.fileSize.sum()] ?: 0L }
            .firstOrNull() ?: 0L
    }

    /**
     * Get total storage used by user within current tenant schema
     */
    suspend fun getTotalStorageByUser(userId: UUID): Long = tenantDbQuery {
        Files.select(Files.fileSize.sum())
            .where { (Files.uploadedBy eq userId) and (Files.deletedAt.isNull()) }
            .map { it[Files.fileSize.sum()] ?: 0L }
            .firstOrNull() ?: 0L
    }

    /**
     * Get all active (not soft-deleted) S3 object keys in the Files table
     */
    suspend fun getAllActiveObjectKeys(): List<String> = tenantDbQuery {
        Files.select(Files.objectKey)
            .where { Files.deletedAt.isNull() }
            .map { it[Files.objectKey] }
    }

    /**
     * Get soft-deleted files older than specified days
     */
    suspend fun getSoftDeletedFiles(olderThanDays: Int = 7): List<FileRecord> = tenantDbQuery {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())
        Files.selectAll()
            .where { Files.deletedAt.isNotNull() and (Files.deletedAt less cutoffDate) }
            .map { it.toFileRecord() }
    }

    /**
     * Hard delete soft-deleted files older than specified days
     */
    suspend fun cleanupSoftDeletedFiles(olderThanDays: Int = 7): Int = tenantDbQuery {
        val cutoffDate = LocalDateTime.now().minusDays(olderThanDays.toLong())
        Files.deleteWhere {
            deletedAt.isNotNull() and (deletedAt less cutoffDate)
        }
    }

    private fun ResultRow.toFileRecord() = FileRecord(
        id = this[Files.id],
        module = this[Files.module],
        type = this[Files.type],
        objectKey = this[Files.objectKey],
        originalFileName = this[Files.originalFileName],
        fileSize = this[Files.fileSize],
        mimeType = this[Files.mimeType],
        uploadedBy = this[Files.uploadedBy],
        createdAt = this[Files.createdAt],
        updatedAt = this[Files.updatedAt],
        deletedAt = this[Files.deletedAt]
    )
}

/**
 * Data class representing a file record from the database
 * No tenantId field - schema isolation provides multi-tenancy
 */
data class FileRecord(
    val id: UUID,
    val module: String,
    val type: String,
    val objectKey: String,
    val originalFileName: String,
    val fileSize: Long,
    val mimeType: String,
    val uploadedBy: UUID,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val deletedAt: LocalDateTime?
)
