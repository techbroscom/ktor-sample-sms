package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Complaints : Table("complaints") {
    val id = varchar("id", 255)
    val title = varchar("title", 500)
    val content = text("content")
    val author = varchar("author", 255)
    val category = varchar("category", 100)
    val status = varchar("status", 50)
    val isAnonymous = bool("is_anonymous")
    val createdAt = datetime("created_at")
    val comments = text("comments") // JSON text for storing comments

    override val primaryKey = PrimaryKey(id)
}