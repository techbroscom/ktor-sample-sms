package com.example.services

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.Exams
import com.example.database.tables.Files
import com.example.database.tables.PostImages
import com.example.database.tables.Tenants
import com.example.database.tables.UserPermissions
import com.example.database.tables.UserDetails
import com.example.database.tables.Visitors
import com.example.database.tables.VisitorPasses
import com.example.database.tables.Books
import com.example.database.tables.BookBorrowings
import com.example.database.tables.BookReservations
import com.example.database.tables.LibraryFines
import com.example.database.tables.LibrarySettings
import com.example.database.tables.LmsCourses
import com.example.database.tables.LmsSections
import com.example.database.tables.LmsBatches
import com.example.database.tables.LmsBatchSections
import com.example.database.tables.LmsBatchSessions
import com.example.database.tables.LmsEnrollments
import com.example.database.tables.LmsConfig
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

    fun migrateTenantFilesTable() {
        println("🔧 Running tenant files table migration (safe mode)...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (files table)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {

                // 🔑 CRITICAL: ensure all operations hit tenant schema
                exec("SET search_path TO $schema")

                // 1️⃣ Check if files table already exists
                val filesTableExists = exec(
                    """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = '$schema'
                      AND table_name = 'files'
                )
                """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (filesTableExists) {
                    println("✓ files table already exists in $schema")
                    return@transaction
                }

                // 2️⃣ Create files table in tenant schema
                println("➕ Creating files table in $schema")
                SchemaUtils.create(Files)

                // 3️⃣ Optional indexes (HIGHLY recommended)
                // NOTE: tenant_id index removed - schema-level isolation, no tenant_id column
                exec("""
                CREATE INDEX IF NOT EXISTS idx_files_uploaded_by
                ON files (uploaded_by)
            """)

                exec("""
                CREATE INDEX IF NOT EXISTS idx_files_module_type
                ON files (module, type)
            """)

                println("✓ files table created successfully in $schema")
            }
        }

        println("✓ Tenant files table migration completed")
    }

    fun migrateTenantPostImagesTable() {
        println("🔧 Running tenant post_images table migration (safe mode)...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (post_images table)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {

                // 🔑 Ensure tenant schema
                exec("SET search_path TO $schema")

                // 1️⃣ Check if posts table exists (dependency check)
                val postsTableExists = exec(
                    """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = '$schema'
                      AND table_name = 'posts'
                )
                """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!postsTableExists) {
                    println("⚠ Skipping $schema (posts table not found)")
                    return@transaction
                }

                // 2️⃣ Check if post_images table already exists
                val postImagesTableExists = exec(
                    """
                SELECT EXISTS (
                    SELECT 1
                    FROM information_schema.tables
                    WHERE table_schema = '$schema'
                      AND table_name = 'post_images'
                )
                """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (postImagesTableExists) {
                    println("✓ post_images table already exists in $schema")
                    return@transaction
                }

                // 3️⃣ Create post_images table
                println("➕ Creating post_images table in $schema")
                SchemaUtils.create(PostImages)

                // 4️⃣ Indexes (important for performance)
                exec("""
                CREATE INDEX IF NOT EXISTS idx_post_images_post_id
                ON post_images (post_id)
            """)

                exec("""
                CREATE INDEX IF NOT EXISTS idx_post_images_display_order
                ON post_images (display_order)
            """)

                println("✓ post_images table created successfully in $schema")
            }
        }

        println("✓ Tenant post_images table migration completed")
    }

    /**
     * Remove tenant_id column from files table
     * Schema-level multi-tenancy provides isolation, tenant_id column is redundant
     */
    fun removeFilesTenantIdColumn() {
        println("🔧 Removing tenant_id column from files table (schema-level isolation)...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (removing files.tenant_id)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if files table exists
                val filesTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'files'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!filesTableExists) {
                    println("⚠ Skipping $schema (files table not found)")
                    return@transaction
                }

                // Check if tenant_id column exists
                val tenantIdColumnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'files'
                          AND column_name = 'tenant_id'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!tenantIdColumnExists) {
                    println("✓ tenant_id column already removed from $schema.files")
                    return@transaction
                }

                // Drop index on tenant_id if it exists
                exec("""
                    DROP INDEX IF EXISTS idx_files_tenant_id
                """)

                // Drop the tenant_id column
                exec("""
                    ALTER TABLE files
                    DROP COLUMN tenant_id
                """)

                println("✓ Removed tenant_id column from $schema.files")
            }
        }

        println("✓ Files table tenant_id column removal completed")
    }

    /**
     * Migrate TenantFeatures table to use feature_id foreign key instead of feature_name
     * Adds new columns: feature_id, custom_limit_value, enabled_at, disabled_at
     * Makes feature_name nullable (deprecated)
     */
    fun migrateTenantFeaturesToNewSchema() {
        println("🔧 Migrating tenant_features table to new schema...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        transaction(systemDb) {
            // Add new columns if they don't exist
            exec("""
                ALTER TABLE tenant_features
                ADD COLUMN IF NOT EXISTS feature_id INTEGER
            """)

            exec("""
                ALTER TABLE tenant_features
                ADD COLUMN IF NOT EXISTS custom_limit_value INTEGER
            """)

            exec("""
                ALTER TABLE tenant_features
                ADD COLUMN IF NOT EXISTS enabled_at TIMESTAMP
            """)

            exec("""
                ALTER TABLE tenant_features
                ADD COLUMN IF NOT EXISTS disabled_at TIMESTAMP
            """)

            // Make feature_name nullable (it's being deprecated)
            exec("""
                ALTER TABLE tenant_features
                ALTER COLUMN feature_name DROP NOT NULL
            """)

            println("✓ TenantFeatures table schema migration completed")
            println("⚠ NOTE: You need to manually populate the Features table and update existing")
            println("  tenant_features records to use feature_id instead of feature_name")
        }
    }

    /**
     * Create UserPermissions table in existing tenant schemas
     */
    fun migrateUserPermissionsTable() {
        println("🔧 Creating user_permissions table in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (user_permissions table)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if user_permissions table already exists
                val tableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'user_permissions'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (tableExists) {
                    println("✓ user_permissions table already exists in $schema")
                    return@transaction
                }

                // Create user_permissions table
                println("➕ Creating user_permissions table in $schema")
                SchemaUtils.create(UserPermissions)

                // Create indexes
                exec("""
                    CREATE INDEX IF NOT EXISTS idx_user_permissions_user_id
                    ON user_permissions (user_id)
                """)

                exec("""
                    CREATE INDEX IF NOT EXISTS idx_user_permissions_feature_id
                    ON user_permissions (feature_id)
                """)

                println("✓ user_permissions table created successfully in $schema")
            }
        }

        println("✓ UserPermissions table migration completed")
    }

    /**
     * Create Visitors and VisitorPasses tables in existing tenant schemas
     */
    fun migrateVisitorManagementTables() {
        println("🔧 Creating visitor management tables in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (visitor management tables)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if visitors table already exists
                val visitorsTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'visitors'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!visitorsTableExists) {
                    // Create visitors table
                    println("➕ Creating visitors table in $schema")
                    SchemaUtils.create(Visitors)

                    // Create indexes
                    exec("""
                        CREATE INDEX IF NOT EXISTS idx_visitors_host_user_id
                        ON visitors (host_user_id)
                    """)

                    exec("""
                        CREATE INDEX IF NOT EXISTS idx_visitors_visit_date
                        ON visitors (visit_date)
                    """)

                    exec("""
                        CREATE INDEX IF NOT EXISTS idx_visitors_status
                        ON visitors (status)
                    """)

                    exec("""
                        CREATE INDEX IF NOT EXISTS idx_visitors_visit_date_status
                        ON visitors (visit_date, status)
                    """)

                    println("✓ visitors table created successfully in $schema")
                } else {
                    println("✓ visitors table already exists in $schema")
                }

                // Check if visitor_passes table already exists
                val visitorPassesTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'visitor_passes'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!visitorPassesTableExists) {
                    // Create visitor_passes table
                    println("➕ Creating visitor_passes table in $schema")
                    SchemaUtils.create(VisitorPasses)

                    // Create indexes
                    exec("""
                        CREATE INDEX IF NOT EXISTS idx_visitor_passes_visitor_id
                        ON visitor_passes (visitor_id)
                    """)

                    println("✓ visitor_passes table created successfully in $schema")
                } else {
                    println("✓ visitor_passes table already exists in $schema")
                }
            }
        }

        println("✓ Visitor management tables migration completed")
    }

    /**
     * Create library management tables in existing tenant schemas
     */
    fun migrateLibraryManagementTables() {
        println("🔧 Creating library management tables in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (library management tables)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Create all library tables
                val tablesToCreate = listOf(
                    Triple(Books, "books", "Books catalog"),
                    Triple(BookBorrowings, "book_borrowings", "Book borrowings"),
                    Triple(BookReservations, "book_reservations", "Book reservations"),
                    Triple(LibraryFines, "library_fines", "Library fines"),
                    Triple(LibrarySettings, "library_settings", "Library settings")
                )

                tablesToCreate.forEach { (table, tableName, description) ->
                    val tableExists = exec(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.tables
                            WHERE table_schema = '$schema'
                              AND table_name = '$tableName'
                        )
                        """
                    ) { rs ->
                        rs.next()
                        rs.getBoolean(1)
                    } ?: false

                    if (!tableExists) {
                        println("➕ Creating $tableName table in $schema")
                        SchemaUtils.create(table)
                        println("✓ $description table created successfully in $schema")
                    } else {
                        println("✓ $tableName table already exists in $schema")
                    }
                }
            }
        }

        println("✓ Library management tables migration completed")
    }

    /**
     * Add photo_url column to users table in existing tenant schemas
     */
    fun migrateUsersPhotoUrl() {
        println("🔧 Adding photo_url column to users table in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (users.photo_url)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if users table exists
                val usersTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!usersTableExists) {
                    println("⚠ Skipping $schema (users table not found)")
                    return@transaction
                }

                // Check if photo_url column already exists
                val photoUrlColumnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                          AND column_name = 'photo_url'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (photoUrlColumnExists) {
                    println("✓ photo_url column already exists in $schema.users")
                    return@transaction
                }

                // Add photo_url column
                exec("""
                    ALTER TABLE users
                    ADD COLUMN photo_url VARCHAR(500)
                """)

                println("✓ Added photo_url column to $schema.users")
            }
        }

        println("✓ Users photo_url column migration completed")
    }

    /**
     * Add S3 storage columns (image_url and image_s3_key) to users table
     * This replaces the deprecated photo_url with a proper S3-based storage solution
     */
    fun migrateUsersS3Columns() {
        println("🔧 Adding image_url and image_s3_key columns to users table in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (users.image_url and users.image_s3_key)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if users table exists
                val usersTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!usersTableExists) {
                    println("⚠ Skipping $schema (users table not found)")
                    return@transaction
                }

                // Check if image_url column already exists
                val imageUrlColumnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                          AND column_name = 'image_url'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!imageUrlColumnExists) {
                    // Add image_url column
                    exec("""
                        ALTER TABLE users
                        ADD COLUMN image_url VARCHAR(500)
                    """)
                    println("✓ Added image_url column to $schema.users")
                } else {
                    println("✓ image_url column already exists in $schema.users")
                }

                // Check if image_s3_key column already exists
                val imageS3KeyColumnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                          AND column_name = 'image_s3_key'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!imageS3KeyColumnExists) {
                    // Add image_s3_key column
                    exec("""
                        ALTER TABLE users
                        ADD COLUMN image_s3_key VARCHAR(500)
                    """)
                    println("✓ Added image_s3_key column to $schema.users")
                } else {
                    println("✓ image_s3_key column already exists in $schema.users")
                }
            }
        }

        println("✓ Users S3 columns migration completed")
    }

    /**
     * Create user_details table in existing tenant schemas
     */
    fun migrateUserDetailsTable() {
        println("🔧 Creating user_details table in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (user_details table)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if users table exists (dependency check)
                val usersTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!usersTableExists) {
                    println("⚠ Skipping $schema (users table not found)")
                    return@transaction
                }

                // Check if user_details table already exists
                val userDetailsTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'user_details'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (userDetailsTableExists) {
                    println("✓ user_details table already exists in $schema")
                    return@transaction
                }

                // Create user_details table
                println("➕ Creating user_details table in $schema")
                SchemaUtils.create(UserDetails)

                // Create index for user_id (used in findByUserId queries)
                exec("""
                    CREATE INDEX IF NOT EXISTS idx_user_details_user_id
                    ON user_details (user_id)
                """)

                println("✓ user_details table created successfully in $schema")
            }
        }

        println("✓ UserDetails table migration completed")
    }

    /**
     * Add logo_s3_key column to school_config table in existing tenant schemas
     * This enables public URL generation for school logos (no expiration)
     */
    fun migrateSchoolConfigS3Column() {
        println("🔧 Adding logo_s3_key column to school_config table in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (school_config.logo_s3_key)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if school_config table exists
                val schoolConfigTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'school_config'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!schoolConfigTableExists) {
                    println("⚠ Skipping $schema (school_config table not found)")
                    return@transaction
                }

                // Check if logo_s3_key column already exists
                val logoS3KeyColumnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'school_config'
                          AND column_name = 'logo_s3_key'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (logoS3KeyColumnExists) {
                    println("✓ logo_s3_key column already exists in $schema.school_config")
                    return@transaction
                }

                // Add logo_s3_key column
                exec("""
                    ALTER TABLE school_config
                    ADD COLUMN logo_s3_key VARCHAR(500)
                """)

                println("✓ Added logo_s3_key column to $schema.school_config")
            }
        }

        println("✓ SchoolConfig logo_s3_key column migration completed")
    }

    /**
     * Remove unique index on email column in users table.
     * Multiple users can share the same email (e.g., parent is both staff and student,
     * or two students under one parent email).
     */
    fun migrateRemoveUsersEmailUniqueIndex() {
        println("🔧 Removing unique index on users.email in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (remove users email unique index)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                // Set search path to tenant schema
                exec("SET search_path TO $schema")

                // Check if users table exists
                val usersTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!usersTableExists) {
                    println("⚠ Skipping $schema (users table not found)")
                    return@transaction
                }

                // Find and drop any unique index on email column
                val indexNames = mutableListOf<String>()
                exec(
                    """
                    SELECT indexname
                    FROM pg_indexes
                    WHERE schemaname = '$schema'
                      AND tablename = 'users'
                      AND indexdef LIKE '%email%'
                      AND indexdef LIKE '%UNIQUE%'
                    """
                ) { rs ->
                    while (rs.next()) {
                        indexNames.add(rs.getString("indexname"))
                    }
                }

                if (indexNames.isEmpty()) {
                    // Also check for unique constraint (not just index)
                    val constraintNames = mutableListOf<String>()
                    exec(
                        """
                        SELECT constraint_name
                        FROM information_schema.table_constraints
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                          AND constraint_type = 'UNIQUE'
                          AND constraint_name LIKE '%email%'
                        """
                    ) { rs ->
                        while (rs.next()) {
                            constraintNames.add(rs.getString("constraint_name"))
                        }
                    }

                    if (constraintNames.isEmpty()) {
                        println("✓ No unique email index/constraint found in $schema.users")
                        return@transaction
                    }

                    // Drop unique constraints
                    constraintNames.forEach { constraintName ->
                        exec("ALTER TABLE users DROP CONSTRAINT IF EXISTS $constraintName")
                        println("✓ Dropped unique constraint '$constraintName' from $schema.users")
                    }
                } else {
                    // In PostgreSQL, unique constraints create backing indexes with the same name.
                    // Must drop as constraint (ALTER TABLE), not as index (DROP INDEX).
                    indexNames.forEach { indexName ->
                        exec("ALTER TABLE users DROP CONSTRAINT IF EXISTS $indexName")
                        println("✓ Dropped unique constraint '$indexName' from $schema.users")
                    }
                }

                // Add a regular (non-unique) index on email for query performance
                exec("""
                    CREATE INDEX IF NOT EXISTS idx_users_email
                    ON users (email)
                """)

                println("✓ Email unique constraint removed, regular index added in $schema")
            }
        }

        println("✓ Users email unique index removal migration completed")
    }

    /**
     * Add tag_line and description columns to school_config table.
     * These allow organizations to set a tagline and about description.
     */
    fun migrateSchoolConfigTagLineAndDescription() {
        println("🔧 Adding tag_line and description columns to school_config table in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (school_config.tag_line, school_config.description)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                exec("SET search_path TO $schema")

                val schoolConfigTableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'school_config'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!schoolConfigTableExists) {
                    println("⚠ Skipping $schema (school_config table not found)")
                    return@transaction
                }

                // Add tag_line column if not exists
                val tagLineExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'school_config'
                          AND column_name = 'tag_line'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!tagLineExists) {
                    exec("ALTER TABLE school_config ADD COLUMN tag_line VARCHAR(255)")
                    println("✓ Added tag_line column to $schema.school_config")
                }

                // Add description column if not exists
                val descriptionExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'school_config'
                          AND column_name = 'description'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!descriptionExists) {
                    exec("ALTER TABLE school_config ADD COLUMN description TEXT")
                    println("✓ Added description column to $schema.school_config")
                }
            }
        }

        println("✓ SchoolConfig tag_line and description migration completed")
    }

    fun migrateUsersAppleRelayEmail() {
        println("🔧 Running Apple relay email column migration...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (users.apple_relay_email)")

            transaction(systemDb) {
                exec("SET search_path TO $schema")

                // Check if column already exists
                val columnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'users'
                          AND column_name = 'apple_relay_email'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!columnExists) {
                    exec("ALTER TABLE users ADD COLUMN apple_relay_email VARCHAR(255) NULL")
                    exec("CREATE INDEX idx_users_apple_relay_email ON users(apple_relay_email) WHERE apple_relay_email IS NOT NULL")
                    println("  ➕ Added apple_relay_email column to $schema.users")
                } else {
                    println("  ⏭ apple_relay_email already exists in $schema.users")
                }
            }
        }

        println("✓ Apple relay email column migration completed")
    }

    /**
     * Create LMS (Learning Management System) tables in existing tenant schemas.
     * Tables: lms_courses, lms_sections, lms_session_templates, lms_batches,
     * lms_batch_sections, lms_batch_sessions, lms_enrollments, lms_config
     */
    fun migrateLmsTables() {
        println("🔧 Creating LMS tables in tenant schemas...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            exec("SET search_path TO public")
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (LMS tables)")

            val tenantDb = TenantDatabaseConfig.getTenantDatabase(schema)

            transaction(tenantDb) {
                exec("SET search_path TO $schema")

                // Order matters due to foreign key dependencies
                val tablesToCreate = listOf(
                    Triple(LmsCourses, "lms_courses", "LMS Courses"),
                    Triple(LmsSections, "lms_sections", "LMS Sections"),
                    Triple(LmsBatches, "lms_batches", "LMS Batches"),
                    Triple(LmsBatchSections, "lms_batch_sections", "LMS Batch Sections"),
                    Triple(LmsBatchSessions, "lms_batch_sessions", "LMS Batch Sessions"),
                    Triple(LmsEnrollments, "lms_enrollments", "LMS Enrollments"),
                    Triple(LmsConfig, "lms_config", "LMS Config")
                )

                tablesToCreate.forEach { (table, tableName, description) ->
                    val tableExists = exec(
                        """
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.tables
                            WHERE table_schema = '$schema'
                              AND table_name = '$tableName'
                        )
                        """
                    ) { rs ->
                        rs.next()
                        rs.getBoolean(1)
                    } ?: false

                    if (!tableExists) {
                        println("➕ Creating $tableName table in $schema")
                        SchemaUtils.create(table)
                        println("✓ $description table created successfully in $schema")
                    } else {
                        println("✓ $tableName table already exists in $schema")
                    }
                }
            }
        }

        println("✓ LMS tables migration completed")
    }

    /**
     * Add provider_meeting_id column to lms_batch_sessions for Zoho Webinar integration.
     * Stores the provider-specific meeting identifier (e.g., "meetingKey::instanceId" for Zoho).
     */
    fun migrateLmsBatchSessionsProviderMeetingId() {
        println("🔧 Adding provider_meeting_id column to lms_batch_sessions...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            exec("SET search_path TO public")
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (lms_batch_sessions.provider_meeting_id)")

            transaction(systemDb) {
                exec("SET search_path TO $schema")

                // Check if column already exists
                val columnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'lms_batch_sessions'
                          AND column_name = 'provider_meeting_id'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (!columnExists) {
                    exec("ALTER TABLE lms_batch_sessions ADD COLUMN provider_meeting_id VARCHAR(255) NULL")
                    println("  ➕ Added provider_meeting_id column to $schema.lms_batch_sessions")
                } else {
                    println("  ⏭ provider_meeting_id already exists in $schema.lms_batch_sessions")
                }
            }
        }

        println("✓ provider_meeting_id column migration completed")
    }

    /**
     * Remove the session_template_id column from lms_batch_sessions and drop
     * the lms_session_templates table. Course hierarchy simplified to:
     * Course → Sections → (Batch Sessions scheduled directly).
     */
    fun migrateLmsRemoveSessionTemplates() {
        println("🔧 Removing session templates layer from LMS...")

        val systemDb = TenantDatabaseConfig.getSystemDb()

        val tenantSchemas = transaction(systemDb) {
            exec("SET search_path TO public")
            Tenants
                .selectAll()
                .map { it[Tenants.schema_name] }
                .filter { it.startsWith("tenant_") }
        }

        tenantSchemas.forEach { schema ->
            println("➡ Migrating schema: $schema (remove session templates)")

            transaction(systemDb) {
                exec("SET search_path TO $schema")

                // 1. Drop session_template_id column from lms_batch_sessions
                val columnExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = '$schema'
                          AND table_name = 'lms_batch_sessions'
                          AND column_name = 'session_template_id'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (columnExists) {
                    exec("ALTER TABLE lms_batch_sessions DROP COLUMN session_template_id")
                    println("  ➖ Dropped session_template_id from $schema.lms_batch_sessions")
                } else {
                    println("  ⏭ session_template_id already removed from $schema.lms_batch_sessions")
                }

                // 2. Drop lms_session_templates table
                val tableExists = exec(
                    """
                    SELECT EXISTS (
                        SELECT 1
                        FROM information_schema.tables
                        WHERE table_schema = '$schema'
                          AND table_name = 'lms_session_templates'
                    )
                    """
                ) { rs ->
                    rs.next()
                    rs.getBoolean(1)
                } ?: false

                if (tableExists) {
                    exec("DROP TABLE lms_session_templates")
                    println("  ➖ Dropped lms_session_templates table from $schema")
                } else {
                    println("  ⏭ lms_session_templates already removed from $schema")
                }
            }
        }

        println("✓ Session templates removal migration completed")
    }

}
