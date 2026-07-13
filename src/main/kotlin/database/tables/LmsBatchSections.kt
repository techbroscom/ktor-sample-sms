package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

/**
 * Per-section pricing within a batch.
 * Allows users to buy individual sections instead of the full batch.
 */
object LmsBatchSections : Table("lms_batch_sections") {
    val id = uuid("id")
    val batchId = uuid("batch_id").references(LmsBatches.id)
    val sectionId = uuid("section_id").references(LmsSections.id)
    val price = decimal("price", 10, 2) // Individual section price for this batch
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(batchId, sectionId) // One entry per section per batch
    }
}
