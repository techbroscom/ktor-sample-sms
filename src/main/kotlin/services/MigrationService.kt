package com.example.services

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.Exams
import com.example.database.tables.Tenants
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.selectAll

class MigrationService {

    fun migrateSystemTables() {
        println("🔧 Running migration for system.tenants...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        transaction(systemDb) {
            SchemaUtils.create(Tenants)

            exec("""
                ALTER TABLE tenants 
                ADD COLUMN IF NOT EXISTS sub_domain VARCHAR(100)
            """)

            exec("""
                ALTER TABLE tenants 
                ALTER COLUMN sub_domain SET NOT NULL
            """)

            println("✓ Migration complete for system.tenants")
        }
    }

    fun migrateTenantExamTables() {
        println("🔧 Running tenant exam migrations (safe mode)...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {

                // 🔑 THIS IS THE MISSING PIECE
                exec("SET search_path TO $schema")

                // 1️⃣ Check if exams table exists in THIS schema
                val examsTableExists = exec("""
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = '$schema'
                      AND table_name = 'exams'
                )
            """) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!examsTableExists) {
                    println("⚠ Skipping $schema (exams table not found)")
                    return@transaction
                }

                // 2️⃣ Now ALTER is guaranteed to hit tenant_xxxx.exams
                exec("""
                ALTER TABLE exams
                ADD COLUMN IF NOT EXISTS result_status VARCHAR(20)
                NOT NULL DEFAULT 'NOT_STARTED'
            """)

                exec("""
                ALTER TABLE exams
                ADD COLUMN IF NOT EXISTS results_published_at TIMESTAMP
            """)
            }
        }

        println("✓ Tenant exam migrations completed safely")
    }


}
