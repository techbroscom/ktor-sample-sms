package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class BookStatus {
    AVAILABLE,
    OUT_OF_STOCK,
    LOST,
    DAMAGED,
    MAINTENANCE
}

object Books : Table("books") {
    val id = uuid("id")
    val isbn = varchar("isbn", 20).nullable()
    val title = varchar("title", 255)
    val author = varchar("author", 255)
    val publisher = varchar("publisher", 255).nullable()
    val publicationYear = integer("publication_year").nullable()
    val edition = varchar("edition", 50).nullable()
    val language = varchar("language", 50).default("English")
    val category = varchar("category", 100)
    val subCategory = varchar("sub_category", 100).nullable()
    val totalCopies = integer("total_copies").default(1)
    val availableCopies = integer("available_copies").default(1)
    val shelfLocation = varchar("shelf_location", 50).nullable()
    val coverImageUrl = varchar("cover_image_url", 500).nullable()
    val description = text("description").nullable()
    val price = decimal("price", 10, 2).nullable()
    val status = enumerationByName("status", 20, BookStatus::class)
    val addedBy = uuid("added_by").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, isbn)
        index(false, category)
        index(false, author)
        index(false, title)
    }
}
