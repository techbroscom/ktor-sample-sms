# User Management Enhancement Migration Guide

This guide explains how to apply the database migrations for the user management enhancements.

## Changes Overview

The user management enhancement includes:
1. **Photo URL column** - Added to `users` table
2. **User Details table** - New table for comprehensive user information

## For Existing Tenants

If you have existing tenant schemas, you need to run migrations to add these features.

### Step 1: Uncomment Migration Lines

In `src/main/kotlin/plugins/Databases.kt`, uncomment the following lines in the `configureDatabases()` function:

```kotlin
runBlocking {
    // ... other migrations ...
    migrationService.migrateUsersPhotoUrl()       // ← Uncomment this
    migrationService.migrateUserDetailsTable()    // ← Uncomment this
}
```

### Step 2: Restart the Application

When you restart the application, the migrations will run automatically for all existing tenant schemas.

**Migration Output:**
```
🔧 Adding photo_url column to users table in tenant schemas...
➡ Migrating schema: tenant_school1 (users.photo_url)
✓ Added photo_url column to tenant_school1.users
➡ Migrating schema: tenant_school2 (users.photo_url)
✓ Added photo_url column to tenant_school2.users
✓ Users photo_url column migration completed

🔧 Creating user_details table in tenant schemas...
➡ Migrating schema: tenant_school1 (user_details table)
➕ Creating user_details table in tenant_school1
✓ user_details table created successfully in tenant_school1
➡ Migrating schema: tenant_school2 (user_details table)
➕ Creating user_details table in tenant_school2
✓ user_details table created successfully in tenant_school2
✓ UserDetails table migration completed
```

### Step 3: Comment Out Migration Lines (Optional)

After successful migration, you can comment out the lines again to prevent them from running on every startup:

```kotlin
runBlocking {
    // ... other migrations ...
//  migrationService.migrateUsersPhotoUrl()       // Already migrated
//  migrationService.migrateUserDetailsTable()    // Already migrated
}
```

## For New Tenants

New tenants created after this update will automatically include:
- `photo_url` column in the `users` table
- Complete `user_details` table

No migration needed! The `createTenantTables()` function in `Databases.kt` has been updated to include `UserDetails`.

## Migration Safety Features

Both migrations include safety checks:
- ✅ **Idempotent** - Can be run multiple times safely
- ✅ **Schema validation** - Checks if tables/columns already exist
- ✅ **Dependency checks** - Verifies required tables exist before migration
- ✅ **Rollback safe** - No data loss if migration fails

## Manual Migration (Alternative)

If you prefer to run migrations manually using SQL, use the script at:
```
docs/user-management-enhancement-setup.sql
```

Connect to each tenant schema and run:
```sql
\c your_database
SET search_path TO tenant_schoolname;
\i docs/user-management-enhancement-setup.sql
```

## Verification

After migration, verify the changes:

```sql
-- Check photo_url column exists
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'photo_url';

-- Check user_details table exists
SELECT table_name
FROM information_schema.tables
WHERE table_name = 'user_details';

-- Check user_details columns
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'user_details'
ORDER BY ordinal_position;
```

## Troubleshooting

### Migration doesn't run
- Ensure migrations are uncommented in `Databases.kt`
- Check application startup logs for errors
- Verify database connection is working

### Column already exists error
- This is safe to ignore - the migration checks if columns exist
- The migration will skip schemas that already have the changes

### Foreign key errors
- Ensure the `users` table exists before running `migrateUserDetailsTable()`
- The migration includes dependency checks

## Need Help?

If you encounter issues:
1. Check the application logs for detailed error messages
2. Verify database connectivity and permissions
3. Ensure all previous migrations have been applied
4. Contact support with the error logs
