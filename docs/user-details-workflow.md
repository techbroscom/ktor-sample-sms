# User Details Workflow Guide

## Overview
User details are **optional** and stored separately from the core user account. This allows flexible user management where not all users need detailed information.

## When to Use User Details

- **Students**: Usually need full details (parents, emergency contacts, etc.)
- **Staff**: May need partial details (emergency contacts, address)
- **Admin**: Typically don't need details

## API Workflow

### 1. Creating a New User

Create the user account first:

```bash
POST /api/v1/users
{
  "email": "student@school.com",
  "mobileNumber": "1234567890",
  "password": "password123",
  "role": "STUDENT",
  "firstName": "John",
  "lastName": "Doe",
  "photoUrl": "https://example.com/photo.jpg"  # Optional
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "6a41748b-6012-4b09-90ec-eba2f3a47283",
    "email": "student@school.com",
    ...
  }
}
```

### 2. Adding User Details

You have **3 options**:

#### Option A: Create Details (Explicit)
```bash
POST /api/v1/user-details
{
  "userId": "6a41748b-6012-4b09-90ec-eba2f3a47283",
  "dateOfBirth": "2010-05-15",
  "gender": "MALE",
  "fatherName": "John Doe Sr.",
  ...
}
```

#### Option B: Upsert Details (Recommended) ⭐
```bash
PUT /api/v1/user-details/user/{userId}/upsert
{
  "dateOfBirth": "2010-05-15",
  "gender": "MALE",
  "fatherName": "John Doe Sr.",
  ...
}
```

**Benefits:**
- ✅ Creates if doesn't exist
- ✅ Updates if exists
- ✅ Single endpoint for both operations
- ✅ Idempotent

#### Option C: Update Details (Requires existing record)
```bash
PUT /api/v1/user-details/user/{userId}
{
  "dateOfBirth": "2010-05-15",
  ...
}
```

### 3. Retrieving User Details

```bash
GET /api/v1/user-details/user/{userId}
```

**If details exist:**
```json
{
  "success": true,
  "data": {
    "id": "...",
    "userId": "6a41748b-6012-4b09-90ec-eba2f3a47283",
    "dateOfBirth": "2010-05-15",
    "gender": "MALE",
    ...
  }
}
```

**If details don't exist:**
```json
{
  "success": false,
  "data": null,
  "message": "User details not found. Please create user details first."
}
```

**Status Code:** 404 (Not Found)

### 4. Deleting User Details

```bash
DELETE /api/v1/user-details/user/{userId}
```

**Note:** Deleting a user automatically deletes their details (CASCADE).

## Best Practices

### For Student Registration

```javascript
// Step 1: Create user account
const userResponse = await createUser({
  email: "student@school.com",
  mobileNumber: "1234567890",
  password: "temp123",
  role: "STUDENT",
  firstName: "John",
  lastName: "Doe"
});

const userId = userResponse.data.id;

// Step 2: Add student details using UPSERT
await fetch(`/api/v1/user-details/user/${userId}/upsert`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    dateOfBirth: "2010-05-15",
    gender: "MALE",
    bloodGroup: "O+",
    nationality: "Indian",
    fatherName: "John Doe Sr.",
    fatherMobile: "0987654321",
    fatherEmail: "father@example.com",
    motherName: "Jane Doe",
    motherMobile: "0987654322",
    emergencyContactName: "Uncle Bob",
    emergencyContactMobile: "0987654323",
    addressLine1: "123 Main St",
    city: "Mumbai",
    state: "Maharashtra",
    postalCode: "400001"
  })
});
```

### For Staff Registration

```javascript
// Create user (staff typically need less details)
const userResponse = await createUser({
  email: "teacher@school.com",
  mobileNumber: "1111111111",
  password: "temp123",
  role: "STAFF",
  firstName: "Mary",
  lastName: "Smith"
});

// Optionally add minimal details
await fetch(`/api/v1/user-details/user/${userResponse.data.id}/upsert`, {
  method: 'PUT',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    emergencyContactName: "Spouse",
    emergencyContactMobile: "2222222222",
    addressLine1: "456 Teacher Ave",
    city: "Mumbai"
  })
});
```

### Checking if Details Exist

```javascript
async function getUserWithDetails(userId) {
  try {
    const response = await fetch(`/api/v1/user-details/user/${userId}`);

    if (response.status === 404) {
      console.log("User details not created yet");
      return null;
    }

    return await response.json();
  } catch (error) {
    console.error("Error fetching user details:", error);
    return null;
  }
}
```

## Index Optimization

We've optimized the database indexes:

**Current Index:**
- ✅ `idx_user_details_user_id` - Used in all queries (findByUserId)

**Removed Indexes** (not currently used):
- ❌ `idx_user_details_date_of_birth` - Would be used for age-based filtering (not implemented)
- ❌ `idx_user_details_gender` - Would be used for gender-based reports (not implemented)

If you need to filter by DOB or gender in the future, you can add these indexes later.

## Common Issues & Solutions

### Issue: "User details not found"

**Cause:** User details were never created for this user.

**Solution:**
1. Use the **upsert endpoint** (recommended)
2. Or check if details exist before trying to update

### Issue: "User details already exist for this user"

**Cause:** Trying to create details when they already exist.

**Solution:** Use the **upsert endpoint** instead of POST

### Issue: Need to update details but not sure if they exist

**Solution:** Always use the **upsert endpoint**:
```bash
PUT /api/v1/user-details/user/{userId}/upsert
```

## Migration Note

For **existing users** created before this feature:
- User details will NOT exist
- You need to create them explicitly
- Use the bulk upsert endpoint or migrate manually
- The `/filter` endpoint returns `details: null` for users without details

## Summary

**Recommended Flow:**
1. Create user with `POST /api/v1/users`
2. Use upsert to add details: `PUT /api/v1/user-details/user/{userId}/upsert`
3. Retrieve with: `GET /api/v1/user-details/user/{userId}`
4. Handle 404 gracefully (details don't exist yet)
