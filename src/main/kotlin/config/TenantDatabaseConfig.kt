package com.example.config

import com.example.tenant.TenantContextHolder
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.TransactionManager
import java.sql.Connection

class TenantDatabaseConfig {
    companion object {
        private val systemDatabase by lazy { initSystemDatabase() }
        private val tenantDatabases = mutableMapOf<String, Database>()

        private fun initSystemDatabase(): Database {
            val config = HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = "jdbc:postgresql://nozomi.proxy.rlwy.net:49285/railway"
                username = "postgres"
                password = "djoekYKIsYrtVRtWoPLMFtnYGikLkwkU"
                schema = "public" // or leave empty, default is public
            }
            return Database.connect(HikariDataSource(config))
        }

        // Remove the explicit getSystemDatabase() function since the property already provides access
        fun getSystemDb(): Database = systemDatabase

        fun getTenantDatabase(schemaName: String): Database {
            return tenantDatabases.getOrPut(schemaName) {
                val config = HikariConfig().apply {
                    driverClassName = "org.postgresql.Driver"
                    jdbcUrl = "jdbc:postgresql://nozomi.proxy.rlwy.net:49285/railway"
                    username = "postgres"
                    password = "djoekYKIsYrtVRtWoPLMFtnYGikLkwkU"
                    schema = schemaName
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