package com.example.config

import io.ktor.server.application.*
import org.jetbrains.exposed.sql.Database

object DatabaseConfig {
    fun init(): Database {
        return Database.connect(
            url = "jdbc:postgresql://ep-silent-brook-a16zf0cf.ap-southeast-1.pg.koyeb.app/koyebdb?user=koyeb-adm&password=npg_BjbAEfSt59Ou",
            user = "koyeb-adm",
            driver = "org.postgresql.Driver",
            password = "npg_BjbAEfSt59Ou",
        )
    }
}