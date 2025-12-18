package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object Files : Table("files") {
    val id = uuid("id")
    val tenantId = varchar("tenant_id", 100) // For multi-tenancy support
    val module = varchar("module", 50) // e.g., "profile", "documents", "assignments"
    val type = varchar("type", 50) // e.g., "image", "pdf", "video"
    val objectKey = varchar("object_key", 500) // S3 object key: tenantId/module/type/filename
    val originalFileName = varchar("original_file_name", 255)
    val fileSize = long("file_size") // in bytes
    val mimeType = varchar("mime_type", 100)
    val uploadedBy = uuid(Users.id.toString()) // User who uploaded the file
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()
    val deletedAt = datetime("deleted_at").nullable() // Soft delete

    override val primaryKey = PrimaryKey(id)
}
