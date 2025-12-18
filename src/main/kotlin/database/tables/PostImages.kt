package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object PostImages : Table("post_images") {
    val id = integer("id").autoIncrement()
    val postId = integer("post_id").references(Posts.id)
    val imageUrl = varchar("image_url", 500)
    val imageS3Key = varchar("image_s3_key", 500)
    val displayOrder = integer("display_order").default(0)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
