package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class UserRole {
    ADMIN, STAFF, STUDENT
}

object Users : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val mobileNumber = varchar("mobile_number", 15)
    val passwordHash = varchar("password_hash", 255)
    val role = enumerationByName("role", 20, UserRole::class)
    val firstName = varchar("first_name", 50)
    val lastName = varchar("last_name", 50)
    val photoUrl = varchar("photo_url", 500).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}