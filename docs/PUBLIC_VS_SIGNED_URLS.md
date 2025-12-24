# Public URLs vs Signed URLs - Complete Guide

## TL;DR - You Were Right!

**You don't need signed URLs for public content like profile pictures and logos.**

I've refactored the code to use **public URLs** instead of signed URLs for:
- ✅ Profile pictures
- ✅ Post images
- ✅ Student dashboard photos

**Result:** No more 401 errors, no URL expiration, simpler architecture!

---

## What Changed

### Before (Signed URLs - ❌ Bad for public content)
```kotlin
// Generated a time-limited signed URL (expires in 60 minutes)
val imageUrl = s3FileService?.generateSignedUrlByKey(imageS3Key, 60)
// Result: https://schoolmate.s3.us-east-005.backblazeb2.com/path/to/file.jpg?X-Amz-Algorithm=...&X-Amz-Expires=3600&X-Amz-Signature=...
// Problem: URL expires after 60 minutes → 401 errors
```

### After (Public URLs - ✅ Good for public content)
```kotlin
// Generates a permanent public URL (never expires)
val imageUrl = s3FileService?.generatePublicUrlByKey(imageS3Key)
// Result: https://schoolmate.s3.us-east-005.backblazeb2.com/path/to/file.jpg
// Benefit: URL never expires, no 401 errors ever!
```

---

## Step 1: Make Your Backblaze B2 Bucket Public

### Option A: Via Backblaze B2 Web Console (Recommended)

1. **Log in to Backblaze B2**: https://secure.backblaze.com/b2_buckets.htm
2. **Find your bucket**: "schoolmate"
3. **Click on the bucket name**
4. **Bucket Settings** → **Bucket Info**
5. **Change "Files in Bucket" to "Public"**
6. **Save**

### Option B: Via Backblaze CLI

```bash
# Install B2 CLI
pip install b2

# Authenticate
b2 authorize-account <applicationKeyId> <applicationKey>

# Make bucket public
b2 update-bucket schoolmate allPublic
```

### Verify It Works

After making the bucket public, test a URL:
```
https://schoolmate.s3.us-east-005.backblazeb2.com/YOUR_TENANT_ID/profile/image/profile_xyz.jpg
```

If it loads without authentication, you're all set! ✅

---

## Step 2: Understanding the Code Changes

### New Methods Available

#### 1. `generatePublicUrl(fileId: String)` - By File ID
```kotlin
// Generate public URL from file ID (looks up S3 key from database)
val url = s3FileService.generatePublicUrl(fileId)
// Returns: https://schoolmate.s3.us-east-005.backblazeb2.com/tenant/profile/image/file.jpg
```

#### 2. `generatePublicUrlByKey(objectKey: String)` - By S3 Key (Fastest)
```kotlin
// Generate public URL directly from S3 object key (no database lookup)
val url = s3FileService.generatePublicUrlByKey("tenant/profile/image/file.jpg")
// Returns: https://schoolmate.s3.us-east-005.backblazeb2.com/tenant/profile/image/file.jpg
```

#### 3. `generateSignedUrl()` - Still Available for Private Content
```kotlin
// For PRIVATE content only (financial records, confidential documents, etc.)
val url = s3FileService.generateSignedUrlByKey(objectKey, expirationMinutes = 60)
// Returns: URL with signature that expires after 60 minutes
```

### Updated Repositories

All repositories now use **public URLs** for public content:

| Repository | Content Type | URL Type | Expires? |
|------------|-------------|----------|----------|
| `UserRepository` | Profile pictures | **Public** | ❌ Never |
| `DashboardRepository` | Student photos | **Public** | ❌ Never |
| `PostImageRepository` | Post images | **Public** | ❌ Never |

---

## Step 3: Fix SchoolConfig Logo (Your Question!)

The SchoolConfig logo currently stores just the URL without tracking the S3 key. Here are your options:

### Option A: Quick Fix - Update Logo Upload to Use Public URLs

When uploading school logo, ensure you're storing the public URL:

```kotlin
// In your logo upload endpoint
val objectKey = s3FileService.uploadFile(...)  // Returns S3 key
val publicUrl = s3FileService.generatePublicUrlByKey(objectKey)  // Generate public URL

// Store public URL in database
schoolConfigService.update(id, UpdateSchoolConfigRequest(
    logoUrl = publicUrl,  // Store the public URL
    ...
))
```

### Option B: Better Long-term - Add S3 Key Tracking to SchoolConfig

Add migration to track S3 keys like Users table does:

```kotlin
// 1. Add column to SchoolConfig table
object SchoolConfig : Table("school_config") {
    val logoUrl = varchar("logo_url", 500).nullable()
    val logoS3Key = varchar("logo_s3_key", 500).nullable()  // Add this
}

// 2. Update repository to generate public URLs
class SchoolConfigRepository(
    private val s3FileService: S3FileService? = null
) {
    private fun mapRowToDto(row: ResultRow): SchoolConfigDto {
        val logoS3Key = row[SchoolConfig.logoS3Key]
        val logoUrl = if (!logoS3Key.isNullOrBlank()) {
            s3FileService?.generatePublicUrlByKey(logoS3Key) ?: row[SchoolConfig.logoUrl]
        } else {
            row[SchoolConfig.logoUrl]
        }

        return SchoolConfigDto(
            logoUrl = logoUrl,
            ...
        )
    }
}
```

---

## When to Use Each Approach

### ✅ Use Public URLs For:
- Profile pictures
- School logos
- Post images (announcements, news)
- Any content meant to be publicly viewable
- Content that doesn't require access control

**Benefits:**
- URLs never expire
- Simpler code
- Better performance
- No server overhead for signing

### ⚠️ Use Signed URLs For:
- Private student records
- Financial documents
- Confidential reports
- Medical records
- Any content requiring time-limited access
- Content requiring access control/auditing

**Benefits:**
- Time-limited access
- Secure (can't be shared indefinitely)
- Access control and logging

---

## URL Formats by Provider

### Backblaze B2 (What you're using)
```
https://schoolmate.s3.us-east-005.backblazeb2.com/{objectKey}
```

### AWS S3
```
https://schoolmate.s3.us-east-1.amazonaws.com/{objectKey}
```

### Cloudflare R2
```
https://schoolmate.r2.dev/{objectKey}
```
*(Note: Requires custom domain setup in R2)*

---

## Migration Guide

### Already Have Users with Expired URLs?

The code automatically handles this! When a user with an `imageS3Key` is fetched:

```kotlin
// If imageS3Key exists, generate fresh public URL
val imageUrl = if (!imageS3Key.isNullOrBlank()) {
    s3FileService?.generatePublicUrlByKey(imageS3Key)  // Fresh public URL
} else {
    row[Users.imageUrl]  // Fallback to stored URL
}
```

**No database migration needed!** The code generates fresh URLs on-the-fly.

---

## Security Considerations

### Q: Won't making the bucket public expose everything?
**A:** Only if files are meant to be private. For a school app:
- ✅ Profile pictures → Public is fine
- ✅ School logos → Public is fine
- ✅ Post images → Public is fine
- ❌ Student records → Use signed URLs
- ❌ Financial data → Use signed URLs

### Q: What about private content?
**A:** Keep using `generateSignedUrl()` for private content. The public URL methods are **additional**, not replacements.

### Q: Can people guess URLs and access files?
**A:**
- UUID-based filenames make guessing impossible: `profile_29f0e59d-f0ed-48ba-bc17-f2bbde6dd5a4_1766555187301.jpg`
- Even if public, files are not discoverable without knowing the exact path
- For truly sensitive content, use signed URLs

---

## Testing

### Test Public URLs Work

```bash
# Replace with actual object key from your database
curl -I "https://schoolmate.s3.us-east-005.backblazeb2.com/d57874f2-b0ab-4b79-91dd-742337558ee1/profile/image/profile_29f0e59d-f0ed-48ba-bc17-f2bbde6dd5a4_1766555187301.jpg"

# Should return 200 OK (not 401)
HTTP/1.1 200 OK
Content-Type: image/jpeg
```

### Test API Endpoints

```bash
# Test student dashboard - should return public photoUrl
curl "http://localhost:8080/api/v1/dashboard/students/complete/{studentId}"

# Response should include:
{
  "photoUrl": null,
  "imageUrl": "https://schoolmate.s3.us-east-005.backblazeb2.com/...",  # No query params!
  "imageS3Key": "d57874f2-.../profile/image/profile_...jpg"
}
```

---

## Troubleshooting

### Still Getting 401 Errors?

1. **Check bucket is public:**
   ```bash
   curl -I "https://schoolmate.s3.us-east-005.backblazeb2.com/test.txt"
   # If 401 → Bucket not public yet
   # If 404 → Bucket is public (file just doesn't exist)
   ```

2. **Check URL format (no query parameters):**
   ```
   ✅ Good: https://schoolmate.s3.us-east-005.backblazeb2.com/path/file.jpg
   ❌ Bad:  https://schoolmate.s3.us-east-005.backblazeb2.com/path/file.jpg?X-Amz-...
   ```

3. **Check object key is correct:**
   - Look in database: `SELECT imageS3Key FROM users WHERE id = '...'`
   - Verify file exists in S3/B2 console

### Public URLs Not Working?

If public URLs don't work after making bucket public:
- Wait 5-10 minutes for DNS/cache propagation
- Check Backblaze status page
- Try alternative format: `https://s3.us-east-005.backblazeb2.com/schoolmate/{objectKey}`

---

## Summary

✅ **Done:**
- Added public URL support to FileStorage interface
- Updated UserRepository to use public URLs
- Updated DashboardRepository to use public URLs
- Updated PostImageRepository to use public URLs
- Committed and pushed changes

📋 **Your TODO:**
1. **Make Backblaze B2 "schoolmate" bucket public** (via web console)
2. **Test profile picture URLs** (should work without 401 errors)
3. **Optionally:** Update SchoolConfig to track S3 keys for logo

🎯 **Result:**
- No more 401 errors
- URLs never expire
- Simpler, faster architecture
- Public content is truly public!

---

## Questions?

- Need help with SchoolConfig migration? Let me know!
- Want to keep some content private with signed URLs? We can set up separate buckets/paths
- Other S3 providers (AWS, Cloudflare)? The code supports all of them!
