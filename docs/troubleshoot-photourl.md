# PhotoUrl Troubleshooting Guide

## Issue
PhotoUrl is not appearing in API responses even though the table has been updated.

## Possible Causes & Solutions

### 1. Migration Not Run

**Check if migration has been applied:**

```sql
-- Check if photo_url column exists
SELECT column_name, data_type
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'photo_url';
```

**If column doesn't exist:**

Option A: Run the migration automatically
```kotlin
// Uncomment this line in src/main/kotlin/plugins/Databases.kt
migrationService.migrateUsersPhotoUrl()
```

Option B: Run SQL manually
```sql
SET search_path TO tenant_yourschema;
ALTER TABLE users ADD COLUMN photo_url VARCHAR(500);
```

### 2. Existing Users Have NULL Values

**This is normal!** Existing users will have `photoUrl: null` in the response.

**Example Response:**
```json
{
  "success": true,
  "data": {
    "id": "6a41748b-6012-4b09-90ec-eba2f3a47283",
    "email": "student@school.com",
    "firstName": "John",
    "lastName": "Doe",
    "photoUrl": null,  // ← This is expected for existing users
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": null
  }
}
```

**To add a photo URL:**

```bash
PUT /api/v1/users/{userId}
{
  "email": "student@school.com",
  "mobileNumber": "1234567890",
  "role": "STUDENT",
  "firstName": "John",
  "lastName": "Doe",
  "photoUrl": "https://example.com/photos/user.jpg"  // ← Add this
}
```

### 3. PhotoUrl Field Missing from Response

**If the field is completely missing** (not even `null`), check:

#### Test the endpoint:

```bash
# Get a specific user
GET http://localhost:8080/api/v1/users/{userId}

# Expected response should include photoUrl field:
{
  "success": true,
  "data": {
    "id": "...",
    "email": "...",
    "firstName": "...",
    "lastName": "...",
    "photoUrl": null,     // ← This field should be present
    ...
  }
}
```

#### Verify the code is deployed:

1. Check `Users.kt` has the photoUrl field:
```kotlin
val photoUrl = varchar("photo_url", 500).nullable()
```

2. Check `UserDto.kt` has the photoUrl field:
```kotlin
val photoUrl: String? = null,
```

3. Check `UserRepository.kt` maps the photoUrl:
```kotlin
photoUrl = row[Users.photoUrl],
```

4. **Restart the application** after code changes!

### 4. JSON Serialization Issue

**If using custom JSON configuration**, ensure nullable fields are included:

```kotlin
// In your serialization config, ensure:
install(ContentNegotiation) {
    json(Json {
        encodeDefaults = true  // Include default/null values
        explicitNulls = true   // Include null fields
    })
}
```

## Quick Test

### 1. Create a new user with photoUrl:

```bash
POST /api/v1/users
{
  "email": "test@example.com",
  "mobileNumber": "9999999999",
  "password": "test123",
  "role": "STUDENT",
  "firstName": "Test",
  "lastName": "User",
  "photoUrl": "https://example.com/photo.jpg"
}
```

### 2. Retrieve the user:

```bash
GET /api/v1/users/{userId}
```

**Expected Response:**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "photoUrl": "https://example.com/photo.jpg",  // ← Should be here!
    ...
  }
}
```

### 3. Update an existing user's photoUrl:

```bash
PUT /api/v1/users/{userId}
{
  "email": "student@school.com",
  "mobileNumber": "1234567890",
  "role": "STUDENT",
  "firstName": "John",
  "lastName": "Doe",
  "photoUrl": "https://example.com/updated-photo.jpg"
}
```

## Common Mistakes

### ❌ Mistake 1: Not restarting the application
After code changes, you must restart the Ktor application.

### ❌ Mistake 2: Looking at old data
Existing users will have `photoUrl: null` until you update them.

### ❌ Mistake 3: Wrong schema
Make sure you're connected to the correct tenant schema when checking the database.

### ❌ Mistake 4: Migration not run
Check that you've uncommented and run the migration in `Databases.kt`

## Verification Checklist

- [ ] Database column exists (`photo_url VARCHAR(500)`)
- [ ] Column is in the correct tenant schema
- [ ] Users.kt model has `photoUrl` field
- [ ] UserDto.kt has `photoUrl` field
- [ ] UserRepository.kt maps `photoUrl` in `mapRowToDto()`
- [ ] Application has been restarted after code changes
- [ ] Testing with newly created user (or updated existing user)
- [ ] API response includes `photoUrl` field (even if null)

## Still Not Working?

### Check application logs:

Look for errors like:
```
Column not found: photo_url
```
This means the migration wasn't run.

### Check database directly:

```sql
SET search_path TO tenant_yourschema;

-- Should return the photo_url column
\d users

-- Or
SELECT * FROM users WHERE id = 'your-user-id' \gx
```

### Enable debug logging:

Add to your application:
```kotlin
println("PhotoUrl value: ${row[Users.photoUrl]}")
```

## Need Help?

If photoUrl is still not appearing:

1. Run the verification SQL script: `docs/verify-photourl-column.sql`
2. Check which tenant schema you're using
3. Verify the migration ran successfully
4. Restart the application
5. Test with a brand new user (not an existing one)

## Summary

**Most common issue:** Migration not run yet
**Solution:** Uncomment `migrationService.migrateUsersPhotoUrl()` in Databases.kt and restart

**Second most common:** Looking at existing users
**Solution:** Existing users will have `null` - this is expected. Update them or create new users with photoUrl.
