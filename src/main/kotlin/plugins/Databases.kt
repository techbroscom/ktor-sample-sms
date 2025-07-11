package com.example.plugins

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.*
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    // Initialize system database
    val systemDatabase = TenantDatabaseConfig.getSystemDb()

    // Create system tables (only Tenants table in public schema)
    transaction(systemDatabase) {
        SchemaUtils.create(
            Tenants // Only tenant management table in system database
        )
    }

    // Note: Tenant-specific tables will be created when each tenant is created
    // through the TenantService.createTenant() method
}

// Utility function to create tenant tables
fun createTenantTables(tenantDatabase: org.jetbrains.exposed.sql.Database) {
    transaction(tenantDatabase) {
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
}