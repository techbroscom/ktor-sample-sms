package com.example.config

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun init(): Database {
        return Database.connect(
            url = "jdbc:postgresql://dpg-d0q8diqli9vc73b9bhmg-a.singapore-postgres.render.com/sms_oeun",
            user = "sms",
            driver = "org.postgresql.Driver",
            password = "eZmti5NHOvT5jqrvK4k4rIAx5BF8v9Hd",
        )
    }
}