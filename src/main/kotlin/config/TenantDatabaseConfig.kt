package com.example.config

import com.example.tenant.TenantContextHolder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

class TenantDatabaseConfig {
    companion object {

        private val systemDatabase by lazy { initSystemDatabase() }
        private val tenantDatabases = mutableMapOf<String, Database>()

        // 🔵 Production DB (inside VPS)
        private const val DB_URL = "jdbc:postgresql://localhost:5433/saas_db"
        private const val DB_USER = "postgres"
        private const val DB_PASS = "smstechdb"

        private fun initSystemDatabase(): Database {
            val config = HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = DB_URL
                username = DB_USER
                password = DB_PASS
                schema = "public"
                maximumPoolSize = 5
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            }
            return Database.connect(HikariDataSource(config))
        }

        fun getSystemDb(): Database = systemDatabase

        fun getTenantDatabase(schemaName: String): Database {
            return tenantDatabases.getOrPut(schemaName) {
                val config = HikariConfig().apply {
                    driverClassName = "org.postgresql.Driver"
                    jdbcUrl = DB_URL
                    username = DB_USER
                    password = DB_PASS
                    schema = schemaName
                    maximumPoolSize = 5
                    isAutoCommit = false
                    transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                }
                Database.connect(HikariDataSource(config))
            }
        }

        fun getCurrentTenantDatabase(): Database {
            val tenant = TenantContextHolder.getTenant()
                ?: throw IllegalStateException("No tenant context found")
            return getTenantDatabase(tenant.schemaName)
        }
    }
}