package com.example.config

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun init(): Database {
        return Database.connect(
            url = "jdbc:postgresql://dpg-d1keclh5pdvs73anec8g-a.singapore-postgres.render.com/sms_d7bq?sslmode=require",
            user = "sms",
            driver = "org.postgresql.Driver",
            password = "zN1HDRM3RfVsEfUivkO0jxdizxPdqs4o"
        )
    }
}