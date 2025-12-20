package com.example.plugins

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.*
import com.example.services.MigrationService
import io.ktor.server.application.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    // Initialize system database
    val systemDatabase = TenantDatabaseConfig.getSystemDb()

    // Create system tables (tenant management tables in public schema)
    transaction(systemDatabase) {
        SchemaUtils.create(
            Tenants,           // Basic tenant information
            TenantConfig,      // Extended tenant configuration (subscription, storage, limits)
            Features,          // Master features catalog
            TenantFeatures     // Tenant feature assignments
        )
    }

    // Note: Tenant-specific tables will be created when each tenant is created
    // through the TenantService.createTenant() method
    // Run migration for existing tenants on startup
    val migrationService = MigrationService()
    runBlocking {
        migrationService.migrateTenantPostImagesTable()
        migrationService.removeFilesTenantIdColumn() // Remove tenant_id column (schema-level isolation)
        migrationService.migrateTenantFeaturesToNewSchema() // Migrate TenantFeatures to use feature_id
        migrationService.migrateUserPermissionsTable() // Create UserPermissions table in tenant schemas
        migrationService.migrateVisitorManagementTables() // Create Visitors and VisitorPasses tables in tenant schemas
        migrationService.migrateLibraryManagementTables() // Create library tables in tenant schemas
    }
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
            FCMTokens,
            StudentFees,
            FeePayments,
            Files,
            UserPermissions,  // User-level feature permissions (per tenant)
            Visitors,         // Visitor management
            VisitorPasses,    // Visitor passes (optional)
            Books,            // Library management
            BookBorrowings,
            BookReservations,
            LibraryFines,
            LibrarySettings
        )
    }
}