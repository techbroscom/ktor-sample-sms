# File Storage Implementation

## Overview

This implementation provides a flexible, S3-compatible file storage abstraction for the Ktor application with support for multiple storage providers including Backblaze B2, AWS S3, and Cloudflare R2.

## Features

- **S3-Compatible Storage Abstraction**: Works with any S3-compatible provider
- **Enforced Folder Structure**: All files follow the pattern `tenantId/module/type/filename`
- **Database Metadata Storage**: Only object keys are stored in PostgreSQL, not full URLs
- **Signed URLs**: Generate temporary signed URLs for secure file access
- **Multi-Provider Support**: Easily switch between Backblaze B2, AWS S3, and Cloudflare R2
- **File Tracking**: Complete audit trail with upload timestamps, file sizes, and user tracking
- **Multi-Tenant Support**: Integrated with the tenant system

## Architecture

### Components

1. **FileStorage Interface** (`services/storage/FileStorage.kt`)
   - Abstract interface for storage operations
   - Methods: uploadFile, deleteFile, generateSignedUrl, fileExists, getFileMetadata

2. **S3CompatibleStorage** (`services/storage/S3CompatibleStorage.kt`)
   - Concrete implementation using AWS SDK
   - Works with Backblaze B2, AWS S3, Cloudflare R2

3. **S3StorageConfig** (`config/S3StorageConfig.kt`)
   - Configuration management for different providers
   - Factory methods for each provider

4. **Files Table** (`database/tables/Files.kt`)
   - Stores file metadata (NOT file content)
   - Fields: id, tenantId, module, type, objectKey, originalFileName, fileSize, mimeType, uploadedBy, timestamps

5. **FileRepository** (`repositories/FileRepository.kt`)
   - Database operations for file metadata
   - CRUD operations, queries by tenant/user/module/type

6. **S3FileService** (`services/S3FileService.kt`)
   - Business logic layer
   - Enforces folder structure, validates files, manages metadata

## Folder Structure

All files are stored with the enforced structure:

```
{tenantId}/{module}/{type}/{filename}
```

**Examples:**
- `school-123/profile/image/profile_uuid_timestamp.jpg`
- `school-123/documents/assignments/assignments_uuid_timestamp.pdf`
- `default/profile/image/profile_uuid_timestamp.png`

### Path Components

- **tenantId**: Organization/school identifier (from X-Tenant header or "default")
- **module**: Feature module (e.g., "profile", "documents", "assignments")
- **type**: File category (e.g., "image", "pdf", "video", "other")
- **filename**: Unique filename with timestamp

## Current Configuration

### Backblaze B2 Settings

```kotlin
accessKeyId: 005627b76e5aa4b0000000001
applicationKey: K005COF4ZYJ1fXZnwgnuE/nsxyUwpBo
region: us-west-002
bucketName: ktor-backend
endpoint: https://s3.us-west-002.backblazeb2.com
```

## API Endpoints

All S3 file endpoints are prefixed with `/api/v1/s3-files`

### Upload Profile Picture
```
POST /api/v1/s3-files/upload/profile
Headers:
  X-Tenant: school-123 (optional, defaults to "default")
Content-Type: multipart/form-data

Parameters:
- userId (required): User UUID or ID string
- file (required): Image file (max 10MB, JPG/PNG/GIF/WebP only)

Response:
{
  "success": true,
  "fileUrl": "https://signed-url...",
  "fileName": "profile_uuid_timestamp.jpg",
  "fileSize": 123456,
  "message": "File uploaded successfully"
}
```

### Upload Document
```
POST /api/v1/s3-files/upload/document
Headers:
  X-Tenant: school-123 (optional, defaults to "default")
Content-Type: multipart/form-data

Parameters:
- userId (required): User UUID or ID string
- category (optional): Document category (defaults to "general")
- file (required): Any file (max 10MB)

Response: Same as profile picture
```

### Get Signed URL
```
GET /api/v1/s3-files/signed-url/{fileId}?expiration=60

Response:
{
  "signedUrl": "https://presigned-url...",
  "expiresIn": "60 minutes"
}
```

### Get File Metadata
```
GET /api/v1/s3-files/metadata/{fileId}

Response:
{
  "id": "uuid",
  "tenantId": "school-123",
  "module": "profile",
  "type": "image",
  "objectKey": "school-123/profile/image/file.jpg",
  "originalFileName": "photo.jpg",
  "fileSize": 123456,
  "mimeType": "image/jpeg",
  "uploadedBy": "user-uuid",
  "createdAt": "2025-01-01T00:00:00",
  "updatedAt": null,
  "deletedAt": null
}
```

### Get Files by User
```
GET /api/v1/s3-files/user/{userId}

Response:
{
  "files": [...],
  "count": 5
}
```

### Get Storage Usage
```
GET /api/v1/s3-files/storage/user/{userId}

Response:
{
  "totalBytes": 5242880,
  "totalMB": "5.00"
}
```

### Delete File
```
DELETE /api/v1/s3-files/{filePath}

Response:
{
  "success": true,
  "message": "File deleted successfully"
}
```

### Test Storage Connection
```
GET /api/v1/s3-files/test-storage

Response:
{
  "result": "Connection successful"
}
```

## Switching Storage Providers

### To AWS S3

```kotlin
val s3StorageConfig = S3StorageConfig.forAwsS3(
    accessKeyId = "YOUR_AWS_KEY",
    secretAccessKey = "YOUR_AWS_SECRET",
    region = "us-east-1",
    bucketName = "your-bucket"
)
```

### To Cloudflare R2

```kotlin
val s3StorageConfig = S3StorageConfig.forCloudflareR2(
    accessKeyId = "YOUR_R2_KEY",
    secretAccessKey = "YOUR_R2_SECRET",
    accountId = "your-account-id",
    bucketName = "your-bucket"
)
```

### Using Environment Variables

```kotlin
// Set these environment variables:
// STORAGE_PROVIDER=BACKBLAZE_B2|AWS_S3|CLOUDFLARE_R2
// STORAGE_ACCESS_KEY_ID=your-key
// STORAGE_SECRET_ACCESS_KEY=your-secret
// STORAGE_REGION=us-west-002
// STORAGE_BUCKET_NAME=your-bucket
// STORAGE_ENDPOINT=https://... (optional, auto-detected for known providers)

val s3StorageConfig = S3StorageConfig.fromEnvironment()
```

## Database Schema

```sql
CREATE TABLE files (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    module VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    uploaded_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    deleted_at TIMESTAMP
);

CREATE INDEX idx_files_tenant ON files(tenant_id);
CREATE INDEX idx_files_module_type ON files(module, type);
CREATE INDEX idx_files_uploaded_by ON files(uploaded_by);
CREATE INDEX idx_files_object_key ON files(object_key);
```

## Security Features

1. **Path Sanitization**: All path components are sanitized to prevent path traversal attacks
2. **File Validation**: Image files are validated by MIME type and file extension
3. **Size Limits**: Maximum file size of 10MB (configurable)
4. **Signed URLs**: Temporary URLs with configurable expiration (default: 60 minutes)
5. **Soft Deletes**: Files are soft-deleted in the database for audit trails
6. **Tenant Isolation**: Files are organized by tenant ID

## Benefits

1. **Vendor Independence**: Easy to switch between storage providers
2. **Cost Optimization**: Use cheaper providers like Backblaze B2
3. **Security**: Signed URLs instead of public URLs
4. **Auditing**: Complete file history in database
5. **Scalability**: S3-compatible storage scales automatically
6. **Multi-tenancy**: Built-in tenant isolation
7. **Coexistence**: Works alongside existing LocalFileService

## Migration from Old System

The system now has TWO file storage options:

1. **LocalFileService** (existing):
   - Routes: `/api/v1/files/*`
   - Stores files on local filesystem

2. **S3FileService** (new):
   - Routes: `/api/v1/s3-files/*`
   - Stores files in S3-compatible storage (Backblaze B2)
   - Database metadata tracking
   - Signed URLs with expiration

Both systems coexist, allowing gradual migration.

## Testing

Test the S3 storage connection:

```bash
curl http://localhost:8080/api/v1/s3-files/test-storage
```

Upload a profile picture:

```bash
curl -X POST http://localhost:8080/api/v1/s3-files/upload/profile \
  -H "X-Tenant: school-123" \
  -F "userId=your-user-uuid" \
  -F "file=@photo.jpg"
```

## Future Enhancements

1. **File Versioning**: Keep multiple versions of files
2. **Batch Operations**: Upload/delete multiple files at once
3. **Image Processing**: Automatic thumbnails and resizing
4. **Storage Quotas**: Per-tenant and per-user storage limits
5. **CDN Integration**: CloudFront or Cloudflare CDN for faster delivery
6. **Encryption**: Client-side or server-side encryption
7. **Virus Scanning**: Integrate antivirus scanning on upload
8. **Webhooks**: Notify external systems on file events

## Support

For issues or questions:
1. Check the logs for detailed error messages
2. Verify Backblaze B2 credentials and bucket configuration
3. Ensure database migrations have run (Files table created)
4. Test connection using the `/test-storage` endpoint
