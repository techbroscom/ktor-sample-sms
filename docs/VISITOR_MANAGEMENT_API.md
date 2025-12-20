# Visitor Management API Documentation

## Overview
The Visitor Management feature allows tenants to track and manage visitors, including registration, check-in/out, and host assignments.

## Setup

### 1. Add Feature to Catalog
Run the SQL script to add the visitor_management feature to the Features table:
```bash
psql -h your-host -U your-user -d your-database -f docs/visitor-management-feature-setup.sql
```

### 2. Enable Feature for Tenant
Use the Tenant Config API to enable the feature for a specific tenant:
```bash
POST /api/v1/tenantsConfig/{tenantId}/features
Content-Type: application/json
X-Tenant: {tenantId}

{
  "featureKey": "visitor_management",
  "customLimitValue": 500
}
```

### 3. Assign Permission to Users
Assign the visitor_management feature to specific users (ADMIN or STAFF):
```bash
POST /api/v1/users/{userId}/permissions
Content-Type: application/json
X-Tenant: {tenantId}

{
  "featureIds": [<visitor_management_feature_id>]
}
```

## API Endpoints

### 1. Create Visitor
**POST** `/api/v1/visitors`

Creates a new visitor record.

**Headers:**
- `X-Tenant`: Tenant ID (UUID)
- `X-User-Id`: User ID creating the visitor (UUID)

**Request Body:**
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "mobileNumber": "+1234567890",
  "organizationName": "Acme Corp",
  "purposeOfVisit": "Business meeting with management",
  "visitDate": "2025-12-25",
  "expectedCheckInTime": "2025-12-25T10:00:00",
  "hostUserId": "host-user-uuid",
  "identificationProof": "Driving License",
  "identificationNumber": "DL123456",
  "photoUrl": "https://s3.amazonaws.com/bucket/photo.jpg",
  "notes": "VIP visitor"
}
```

**Response:** `201 Created`
```json
{
  "success": true,
  "data": {
    "id": "visitor-uuid",
    "firstName": "John",
    "lastName": "Doe",
    "fullName": "John Doe",
    "email": "john.doe@example.com",
    "mobileNumber": "+1234567890",
    "organizationName": "Acme Corp",
    "purposeOfVisit": "Business meeting with management",
    "visitDate": "2025-12-25",
    "expectedCheckInTime": "2025-12-25T10:00:00",
    "actualCheckInTime": null,
    "checkOutTime": null,
    "status": "SCHEDULED",
    "hostUser": {
      "id": "host-user-uuid",
      "firstName": "Jane",
      "lastName": "Smith",
      "email": "jane.smith@example.com",
      "role": "STAFF"
    },
    "identificationProof": "Driving License",
    "identificationNumber": "DL123456",
    "photoUrl": "https://s3.amazonaws.com/bucket/photo.jpg",
    "notes": "VIP visitor",
    "createdAt": "2025-12-20T08:00:00",
    "updatedAt": null,
    "createdBy": {
      "id": "creator-uuid",
      "firstName": "Admin",
      "lastName": "User",
      "email": "admin@example.com",
      "role": "ADMIN"
    }
  },
  "message": "Visitor created successfully"
}
```

---

### 2. Search/List Visitors
**GET** `/api/v1/visitors`

Search and filter visitors with pagination.

**Query Parameters:**
- `search`: Search by name, email, or mobile number
- `visitDate`: Filter by specific visit date (YYYY-MM-DD)
- `status`: Filter by status (SCHEDULED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW)
- `hostUserId`: Filter by host user
- `fromDate`: Filter visits from this date (YYYY-MM-DD)
- `toDate`: Filter visits until this date (YYYY-MM-DD)
- `page`: Page number (default: 1)
- `pageSize`: Items per page (default: 20)

**Example:**
```
GET /api/v1/visitors?status=CHECKED_IN&page=1&pageSize=20
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [...],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "totalItems": 50,
    "totalPages": 3
  }
}
```

---

### 3. Get Visitor by ID
**GET** `/api/v1/visitors/{id}`

Retrieve a specific visitor by ID.

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "visitor-uuid",
    ...
  }
}
```

---

### 4. Update Visitor
**PUT** `/api/v1/visitors/{id}`

Update visitor details (cannot update CHECKED_OUT visitors).

**Request Body:** (all fields optional)
```json
{
  "firstName": "John",
  "lastName": "Doe Updated",
  "email": "john.updated@example.com",
  "mobileNumber": "+1234567899",
  "organizationName": "New Corp",
  "purposeOfVisit": "Updated purpose",
  "visitDate": "2025-12-26",
  "expectedCheckInTime": "2025-12-26T11:00:00",
  "hostUserId": "new-host-uuid",
  "notes": "Updated notes"
}
```

**Response:** `200 OK`

---

### 5. Check In Visitor
**POST** `/api/v1/visitors/{id}/check-in`

Check in a scheduled visitor.

**Request Body:** (all fields optional)
```json
{
  "actualCheckInTime": "2025-12-25T10:15:00",
  "photoUrl": "https://s3.amazonaws.com/bucket/checkin-photo.jpg",
  "passNumber": "PASS-12345"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "visitor-uuid",
    "status": "CHECKED_IN",
    "actualCheckInTime": "2025-12-25T10:15:00",
    ...
  },
  "message": "Visitor checked in successfully"
}
```

---

### 6. Check Out Visitor
**POST** `/api/v1/visitors/{id}/check-out`

Check out a visitor who is currently checked in.

**Request Body:** (all fields optional)
```json
{
  "checkOutTime": "2025-12-25T12:30:00",
  "notes": "Visit completed successfully"
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "visitor-uuid",
    "status": "CHECKED_OUT",
    "checkOutTime": "2025-12-25T12:30:00",
    ...
  },
  "message": "Visitor checked out successfully"
}
```

---

### 7. Cancel Visitor
**POST** `/api/v1/visitors/{id}/cancel`

Cancel a scheduled visitor.

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": "visitor-uuid",
    "status": "CANCELLED",
    ...
  },
  "message": "Visitor cancelled successfully"
}
```

---

### 8. Delete Visitor
**DELETE** `/api/v1/visitors/{id}`

Delete a visitor (only SCHEDULED or CANCELLED visitors can be deleted).

**Response:** `200 OK`
```json
{
  "success": true,
  "data": null,
  "message": "Visitor deleted successfully"
}
```

---

### 9. Get Visitor Statistics
**GET** `/api/v1/visitors/stats`

Get visitor statistics for a date range.

**Query Parameters:**
- `fromDate`: Start date (default: 30 days ago)
- `toDate`: End date (default: today)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "totalVisitors": 150,
    "currentlyCheckedIn": 5,
    "scheduledToday": 10,
    "completedToday": 8,
    "noShowsThisWeek": 2
  }
}
```

---

### 10. Get Currently Checked-In Visitors
**GET** `/api/v1/visitors/checked-in`

Get all visitors who are currently checked in (on premises). Useful for:
- **Reception Dashboard**: Real-time view of who's on premises
- **Emergency Evacuation**: Know exactly who to account for
- **Security Monitoring**: Quick overview of active visitors
- **Capacity Management**: Track current building occupancy

**Query Parameters:**
- `visitDate`: Filter by specific visit date (YYYY-MM-DD). If omitted, returns all checked-in visitors regardless of date.

**Example:**
```
GET /api/v1/visitors/checked-in
GET /api/v1/visitors/checked-in?visitDate=2025-12-25
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": "visitor-uuid-1",
      "firstName": "John",
      "lastName": "Doe",
      "fullName": "John Doe",
      "mobileNumber": "+1234567890",
      "status": "CHECKED_IN",
      "actualCheckInTime": "2025-12-25T10:15:00",
      "visitDate": "2025-12-25",
      "purposeOfVisit": "Business meeting",
      "hostUser": {
        "id": "host-uuid",
        "firstName": "Jane",
        "lastName": "Smith",
        "email": "jane.smith@example.com",
        "role": "STAFF"
      },
      ...
    },
    {
      "id": "visitor-uuid-2",
      "firstName": "Alice",
      "lastName": "Johnson",
      "status": "CHECKED_IN",
      "actualCheckInTime": "2025-12-25T09:30:00",
      ...
    }
  ],
  "message": "All currently checked-in visitors"
}
```

**Notes:**
- Results are ordered by check-in time (most recent first)
- Only returns visitors with status `CHECKED_IN`
- Omit `visitDate` to see all checked-in visitors across all dates (e.g., overnight visitors)

---

### 11. Get My Hosted Visitors
**GET** `/api/v1/visitors/my-hosted`

Get visitors hosted by the current user.

**Headers:**
- `X-User-Id`: Current user ID

**Query Parameters:**
- `status`: Filter by status (optional)

**Response:** `200 OK`
```json
{
  "success": true,
  "data": [...]
}
```

---

## Visitor Status Lifecycle

```
SCHEDULED → CHECKED_IN → CHECKED_OUT
    ↓            ↓
CANCELLED    NO_SHOW
```

**Status Descriptions:**
- `SCHEDULED`: Visitor is pre-registered, not yet arrived
- `CHECKED_IN`: Visitor is currently on premises
- `CHECKED_OUT`: Visit completed
- `CANCELLED`: Scheduled visit was cancelled
- `NO_SHOW`: Scheduled visitor didn't arrive

## Error Responses

### 400 Bad Request
```json
{
  "error": {
    "code": "400",
    "message": "Invalid visitor ID format"
  }
}
```

### 404 Not Found
```json
{
  "error": {
    "code": "404",
    "message": "Visitor not found"
  }
}
```

### 409 Conflict
```json
{
  "error": {
    "code": "400",
    "message": "Only scheduled visitors can be checked in. Current status: CHECKED_IN"
  }
}
```

## Multi-Tenant Considerations

- All visitor data is isolated per tenant schema
- The `X-Tenant` header must be provided in all requests
- Visitors are automatically scoped to the tenant's schema
- Host users must belong to the same tenant

## Best Practices

1. **Pre-Registration**: Register visitors before their visit date
2. **Host Assignment**: Always assign a host (ADMIN or STAFF) to each visitor
3. **Timely Check-In/Out**: Check in visitors when they arrive and check out when they leave
4. **Mobile Validation**: Use international format for mobile numbers (+1234567890)
5. **Photo Upload**: Upload visitor photos to S3 before creating visitor records
6. **Visitor Passes**: Use the VisitorPasses table for physical/digital pass management

## Database Schema

### Visitors Table (Tenant Schema)
- `id` (UUID, PK)
- `first_name`, `last_name`, `email`, `mobile_number`
- `organization_name`, `purpose_of_visit`
- `visit_date`, `expected_check_in_time`
- `actual_check_in_time`, `check_out_time`
- `status` (enum)
- `host_user_id` (FK to Users)
- `identification_proof`, `identification_number`
- `photo_url`, `notes`
- `created_at`, `updated_at`, `created_by`

### Indexes
- `idx_visitors_host_user_id`
- `idx_visitors_visit_date`
- `idx_visitors_status`
- `idx_visitors_visit_date_status`

## Future Enhancements

- Visitor pass QR code generation
- Self-check-in kiosks
- Email/SMS notifications to hosts
- Recurring visitor management
- Bulk visitor import
- Visitor analytics dashboard
