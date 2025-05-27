package com.example.plugins

import com.example.config.DatabaseConfig
import com.example.database.tables.Holidays
import com.example.database.tables.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val database = DatabaseConfig.init()

    // Create tables
    transaction(database) {
        SchemaUtils.create(Users, Holidays)
    }
}