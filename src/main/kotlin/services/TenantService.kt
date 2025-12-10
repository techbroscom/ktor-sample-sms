package com.example.services

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.*
import com.example.tenant.TenantContext
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

class TenantService {

    suspend fun createTenant(name: String, subDomain: String): TenantContext {
        val tenantId = UUID.randomUUID()

        // Step 1: Insert tenant with temporary schema_name
        transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.insert {
                it[id] = tenantId
                it[Tenants.name] = name
                it[Tenants.subDomain] = subDomain
                it[schema_name] = "" // placeholder, to be updated later
            }
        }

        // Step 2: Fetch tenant_number using selectAll().where
        val tenantNumber = transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants
                .selectAll()
                .where { Tenants.id eq tenantId }
                .single()[Tenants.tenantNumber]
        }

        // Step 3: Format schema name like tenant_0001
        val schemaName = "tenant_${tenantNumber.toString().padStart(4, '0')}"

        // Step 4: Update schema_name in system DB
        transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.update({ Tenants.id eq tenantId }) {
                it[Tenants.schema_name] = schemaName
            }
        }

        val tenantContext = TenantContext(
            id = tenantId.toString(),
            name = name,
            subDomain = schemaName,
            schemaName = schemaName
        )

        // Step 5: Create schema and tenant tables
        val tenantDb = TenantDatabaseConfig.getTenantDatabase(schemaName)
        transaction(tenantDb) {
            exec("CREATE SCHEMA IF NOT EXISTS $schemaName")

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
                FCMTokens,
                StudentFees,
                FeePayments,
                TransportRoutes,
                TransportStops,
                StudentTransportAssignments
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
                    subDomain = it[Tenants.subDomain],
                    schemaName = it[Tenants.schema_name]
                )
            }
        }
    }

    suspend fun getTenantBySubdomain(subdomain: String): TenantContext? {
        return transaction(TenantDatabaseConfig.getSystemDb()) {
            Tenants.selectAll()
                .where { Tenants.subDomain eq subdomain }
                .map {
                    TenantContext(
                        id = it[Tenants.id].toString(),
                        name = it[Tenants.name],
                        subDomain = it[Tenants.subDomain],
                        schemaName = it[Tenants.schema_name]
                    )
                }
                .singleOrNull()
        }
    }
}