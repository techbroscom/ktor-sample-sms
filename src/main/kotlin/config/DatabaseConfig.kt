package com.example.config

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun init(): Database {
        return Database.connect(
            url = "jdbc:postgresql://nozomi.proxy.rlwy.net:49285/railway",
            user = "postgres",
            password = "djoekYKIsYrtVRtWoPLMFtnYGikLkwkU",
            driver = "org.postgresql.Driver"
        )
    }
}
