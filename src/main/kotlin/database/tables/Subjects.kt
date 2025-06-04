package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object Subjects : Table("subjects") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val code = varchar("code", 20).uniqueIndex()

    override val primaryKey = PrimaryKey(id)
}