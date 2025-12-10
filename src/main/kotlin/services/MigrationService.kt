package com.example.services

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.Tenants
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class MigrationService {

    fun migrateTenantsTable() {
        println("🔧 Running migration for system.tenants...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        transaction(systemDb) {

            SchemaUtils.create(Tenants)

            // 1. Add column as nullable first
            exec("""
                ALTER TABLE tenants 
                ADD COLUMN IF NOT EXISTS sub_domain VARCHAR(100)
            """)

            // 2. Populate existing rows with a fallback value
            Tenants.update({ Tenants.subDomain.isNull() }) {
                it[subDomain] = "default"
            }

            // 3. Make it NOT NULL
            exec("""
                ALTER TABLE tenants 
                ALTER COLUMN sub_domain SET NOT NULL
            """)

            println("✓ Migration complete for system.tenants")
        }
    }
}
