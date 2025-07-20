package com.example.services

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.ChatMessages
import com.example.database.tables.ChatRoomMembers
import com.example.database.tables.ChatRooms
import com.example.database.tables.FeePayments
import com.example.database.tables.StudentFees
import com.example.database.tables.StudentTransportAssignments
import com.example.database.tables.Tenants
import com.example.database.tables.TransportStops
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class MigrationService {

    suspend fun migrateTransportRoutesTable() {
        println("Starting migration: Adding student_transport_assignments table to existing tenants...")

        // Get all existing tenants
        val tenants = transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.selectAll().map {
                it[Tenants.schema_name]
            }
        }

        println("Found ${tenants.size} tenants to migrate")

        // For each tenant, create the transport_routes table
        tenants.forEach { schemaName ->
            try {
                val tenantDb = TenantDatabaseConfig.getTenantDatabase(schemaName)
                transaction(tenantDb) {
                    // Proper way to check if table exists
                    val tableExists = exec("""
                        SELECT EXISTS (
                            SELECT 1 FROM information_schema.tables 
                            WHERE table_schema = '$schemaName' 
                            AND table_name = 'student_fees, fee_payments'
                        )
                    """) { rs ->
                        rs.next()
                        rs.getBoolean(1)
                    } ?: false

                    println("Checking table existence for $schemaName: $tableExists")

                    if (!tableExists) {
                        SchemaUtils.create(
                            StudentFees,
                            FeePayments,
                            )
                        println("✓ Created student_transport_assignments table for tenant: $schemaName")
                    } else {
                        println("✓ Table already exists for tenant: $schemaName")
                    }
                }
            } catch (e: Exception) {
                println("✗ Failed to migrate tenant $schemaName: ${e.message}")
                e.printStackTrace()
            }
        }

        println("Migration completed!")
    }
}