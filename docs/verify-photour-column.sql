-- Verification Script for PhotoUrl Feature
-- Run this in your database to check if the migration was applied correctly

-- Step 1: Check if photo_url column exists in users table
SELECT
    table_schema,
    table_name,
    column_name,
    data_type,
    character_maximum_length,
    is_nullable
FROM information_schema.columns
WHERE table_name = 'users'
  AND column_name = 'photo_url';

-- Expected result: Should show the photo_url column in each tenant schema
-- If empty, the migration hasn't been run

-- Step 2: Check current values in photo_url column (replace tenant_school1 with your schema)
-- SET search_path TO tenant_school1;
SELECT
    id,
    first_name,
    last_name,
    email,
    photo_url
FROM users
LIMIT 10;

-- Step 3: Test update - Add a photo URL to a user
-- UPDATE users
-- SET photo_url = 'https://example.com/photo.jpg'
-- WHERE id = 'your-user-id-here';

-- Step 4: Verify the update
-- SELECT id, first_name, last_name, photo_url
-- FROM users
-- WHERE id = 'your-user-id-here';
