package com.example.repositories

import com.example.database.tables.Files
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class FileRepository {

    /**
     * Create a new file record within the tenant context
     */
    suspend fun create(
        tenantId: String,
        module: String,
        type: String,
        objectKey: String,
        originalFileName: String,
        fileSize: Long,
        mimeType: String,
        uploadedBy: String
    ): UUID = dbQuery {
        val fileId = UUID.randomUUID()

        Files.insert {
            it[id] = fileId
            it[Files.tenantId] = tenantId
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
    suspend fun findById(fileId: UUID): FileRecord? = dbQuery {
        Files.selectAll()
            .where { (Files.id eq fileId) and (Files.deletedAt.isNull()) }
            .map { it.toFileRecord() }
            .singleOrNull()
    }

    /**
     * Find a file by object key
     */
    suspend fun findByObjectKey(objectKey: String): FileRecord? = dbQuery {
        Files.selectAll()
            .where { (Files.objectKey eq objectKey) and (Files.deletedAt.isNull()) }
            .map { it.toFileRecord() }
            .singleOrNull()
    }

    /**
     * Find all files uploaded by a user
     */
    suspend fun findByUploadedBy(userId: UUID): List<FileRecord> = dbQuery {
        Files.selectAll()
            .where { (Files.uploadedBy eq userId) and (Files.deletedAt.isNull()) }
            .map { it.toFileRecord() }
    }

    /**
     * Find files by tenant, module, and type
     */
    suspend fun findByTenantModuleType(tenantId: String, module: String, type: String): List<FileRecord> = dbQuery {
        Files.selectAll()
            .where {
                (Files.tenantId eq tenantId) and
                        (Files.module eq module) and
                        (Files.type eq type) and
                        (Files.deletedAt.isNull())
            }
            .map { it.toFileRecord() }
    }

    /**
     * Soft delete a file by ID
     */
    suspend fun softDelete(fileId: UUID): Boolean = dbQuery {
        Files.update({ Files.id eq fileId }) {
            it[deletedAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    /**
     * Soft delete a file by object key
     */
    suspend fun softDeleteByObjectKey(objectKey: String): Boolean = dbQuery {
        Files.update({ Files.objectKey eq objectKey }) {
            it[deletedAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    /**
     * Hard delete a file by ID
     */
    suspend fun hardDelete(fileId: UUID): Boolean = dbQuery {
        Files.deleteWhere { id eq fileId } > 0
    }

    /**
     * Update file metadata
     */
    suspend fun update(fileId: UUID, updates: Map<String, Any>): Boolean = dbQuery {
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
     * Get total storage used by tenant
     */
    suspend fun getTotalStorageByTenant(tenantId: String): Long = dbQuery {
        // In newer Exposed, use select() with the columns/aggregations you need directly
        Files.select(Files.fileSize.sum())
            .where { (Files.tenantId eq tenantId) and (Files.deletedAt.isNull()) }
            .map { it[Files.fileSize.sum()] ?: 0L }
            .firstOrNull() ?: 0L
    }

    /**
     * Get total storage used by user
     */
    suspend fun getTotalStorageByUser(userId: UUID): Long = dbQuery {
        Files.select(Files.fileSize.sum())
            .where { (Files.uploadedBy eq userId) and (Files.deletedAt.isNull()) }
            .map { it[Files.fileSize.sum()] ?: 0L }
            .firstOrNull() ?: 0L
    }

    private fun ResultRow.toFileRecord() = FileRecord(
        id = this[Files.id],
        tenantId = this[Files.tenantId],
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
 */
data class FileRecord(
    val id: UUID,
    val tenantId: String,
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