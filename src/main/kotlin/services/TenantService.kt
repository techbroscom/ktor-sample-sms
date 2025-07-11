package com.example.services

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.*
import com.example.tenant.TenantContext
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

class TenantService {

    suspend fun createTenant(name: String): TenantContext {
        val tenantId = UUID.randomUUID()
        val schemaName = "tenant_$name"

        // 1. Create tenant record in system database
        val tenantContext = transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.insert {
                it[id] = tenantId
                it[Tenants.name] = name
                it[Tenants.schema_name] = schemaName
            }

            TenantContext(
                id = tenantId.toString(),
                name = name,
                schemaName = schemaName
            )
        }

        // 2. Create tenant schema and tables
        val tenantDb = TenantDatabaseConfig.getTenantDatabase(schemaName)
        transaction(tenantDb) {
            // Create schema
            exec("CREATE SCHEMA IF NOT EXISTS $schemaName")

            // Create all tables for this tenant
            SchemaUtils.create(
                Users,
                Holidays,
                Posts,
                Complaints,
                OtpCodes,
                SchoolConfig,
                Rules,
                AcademicYears,
                Subjects,
                Classes,
                ClassSubjects,
                StudentAssignments,
                StaffClassAssignments,
                StaffSubjectAssignments,
                Exams,
                ExamSchedules,
                ExamResults,
                Attendance,
                FeesStructures,
                FCMTokens
            )
        }

        return tenantContext
    }

    suspend fun getAllTenants(): List<TenantContext> {
        return transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.selectAll().map {
                TenantContext(
                    id = it[Tenants.id].toString(),
                    name = it[Tenants.name],
                    schemaName = it[Tenants.schema_name]
                )
            }
        }
    }

    /*suspend fun getTenantBySubdomain(subdomain: String): TenantContext? {
        return transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.selectAll()
                .where { Tenants.subdomain eq subdomain }
                .map {
                    TenantContext(
                        id = it[Tenants.id],
                        name = it[Tenants.name],
                        subdomain = it[Tenants.subdomain],
                        schemaName = it[Tenants.schemaName]
                    )
                }
                .singleOrNull()
        }
    }*/
}