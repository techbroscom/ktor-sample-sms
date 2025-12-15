package com.example.config

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun init(): Database {
        // Prefer environment-configured database (JDBC URL or DATABASE_URL).
        val jdbcUrl = System.getenv("JDBC_DATABASE_URL") ?: System.getenv("DATABASE_URL")
        try {
            if (!jdbcUrl.isNullOrBlank()) {
                // Support DATABASE_URL values that may be in the form postgresql://user:pass@host:port/db
                val url = if (jdbcUrl.startsWith("postgres://") || jdbcUrl.startsWith("postgresql://")) {
                    // Convert to JDBC URL
                    jdbcUrl.replaceFirst(Regex("^postgres(?:ql)?://"), "jdbc:postgresql://")
                } else jdbcUrl

                val user = System.getenv("DB_USER")
                val password = System.getenv("DB_PASSWORD")

                return if (!user.isNullOrBlank() && !password.isNullOrBlank()) {
                    Database.connect(url = url, user = user, driver = "org.postgresql.Driver", password = password)
                } else {
                    // Assume credentials are part of the JDBC URL
                    Database.connect(url = url, driver = "org.postgresql.Driver")
                }
            }
        } catch (e: Exception) {
            println("Failed to connect to configured PostgreSQL database: ${e.message}")
            e.printStackTrace()
            println("Falling back to embedded H2 for local development.")
        }

        // Fallback: in-memory H2 for local development/tests
        return Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;", driver = "org.h2.Driver", user = "sa", password = "")
    }
}