# Password Reset Implementation

## Overview
This document describes the in-app OTP-based password reset feature for the SchoolMate application.

## Implementation Details

### Flow
1. User enters **mobile number** on "Forgot Password" screen
2. System finds user's **email** and sends **6-digit OTP**
3. User receives OTP via email (10-minute validity)
4. User enters **OTP + new password** in app
5. System validates OTP and updates password
6. User can now login with new password

### API Endpoints

#### 1. Send Password Reset OTP
```
POST /api/v1/users/forgot-password/send-otp
```

**Request Body:**
```json
{
  "mobileNumber": "9876543210"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "message": "Password reset code sent to your email",
    "mobileNumber": "9876543210",
    "email": "t***@example.com"
  },
  "message": "Password reset code sent successfully"
}
```

#### 2. Reset Password with OTP
```
POST /api/v1/users/forgot-password/reset
```

**Request Body:**
```json
{
  "mobileNumber": "9876543210",
  "otpCode": "123456",
  "newPassword": "newSecurePassword123"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "email": "test@example.com",
    "mobileNumber": "9876543210",
    "firstName": "John",
    "lastName": "Doe",
    ...
  },
  "message": "Password reset successfully"
}
```

### Error Responses

**Invalid Mobile Number:**
```json
{
  "success": false,
  "message": "No account found with this mobile number"
}
```

**Invalid OTP:**
```json
{
  "success": false,
  "message": "Invalid or expired OTP code. 2 attempts remaining."
}
```

**Too Many Attempts:**
```json
{
  "success": false,
  "message": "Too many failed attempts. Please request a new password reset code."
}
```

## Security Features

### 1. OTP Security
- **6-digit random code** (100000-999999)
- **10-minute expiration** window
- **3 attempts maximum** before OTP invalidation
- **One-time use** - OTP marked as used after successful verification
- **Auto-cleanup** of expired OTPs via background job

### 2. Email Privacy
- Email addresses are **masked** in responses (e.g., "t***@example.com")
- Prevents email enumeration attacks

### 3. Password Requirements
- **Minimum 6 characters**
- Hashed using **BCrypt with salt**
- Cannot be blank

### 4. Validation
- Mobile number existence check
- OTP format validation (must be 6 digits)
- Attempt limiting with remaining attempts counter
- Service availability checks

## Email Template

Users receive a professional HTML email with:
- Clear "Password Reset Request" subject
- Large, easy-to-read OTP code
- 10-minute validity warning
- Security notice (if not requested, ignore email)
- SchoolMate branding

**Example Email:**
```
Subject: Password Reset Code - SchoolMate

Password Reset Request

You have requested to reset your password. Use the code below to reset your password:

┌─────────────────┐
│   1 2 3 4 5 6   │
└─────────────────┘

Important: This code is valid for 10 minutes only.

If you did not request a password reset, please ignore this email or contact support if you have concerns.
```

## Database Tables Used

### OtpCodes Table (Reused)
```sql
CREATE TABLE otp_codes (
    id UUID PRIMARY KEY,
    email VARCHAR(255),
    otp_code VARCHAR(6),
    expires_at TIMESTAMP,
    is_used BOOLEAN DEFAULT false,
    attempts INTEGER DEFAULT 0,
    created_at TIMESTAMP
);
```

### Users Table
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    mobile_number VARCHAR(15),
    password_hash VARCHAR(255),
    ...
);
```

## Flutter Integration

### Step 1: Forgot Password Screen
```dart
// User enters mobile number
TextField(
  controller: mobileController,
  decoration: InputDecoration(
    labelText: 'Mobile Number',
    hintText: 'Enter your mobile number'
  ),
)

// Call API
final response = await api.post('/api/v1/users/forgot-password/send-otp', {
  'mobileNumber': mobileController.text,
});

// Show masked email to user
showDialog(
  context: context,
  builder: (context) => AlertDialog(
    title: Text('OTP Sent'),
    content: Text('Code sent to ${response.data.email}'),
  ),
);
```

### Step 2: OTP + New Password Screen
```dart
// User enters OTP and new password
TextField(
  controller: otpController,
  decoration: InputDecoration(labelText: 'Enter OTP'),
  keyboardType: TextInputType.number,
  maxLength: 6,
)

TextField(
  controller: newPasswordController,
  decoration: InputDecoration(labelText: 'New Password'),
  obscureText: true,
)

// Call reset API
final response = await api.post('/api/v1/users/forgot-password/reset', {
  'mobileNumber': mobileController.text,
  'otpCode': otpController.text,
  'newPassword': newPasswordController.text,
});

// Success - navigate to login
Navigator.pushReplacementNamed(context, '/login');
```

## Files Modified

1. **src/main/kotlin/models/dto/OtpDto.kt**
   - Added `ForgotPasswordSendOtpRequest`
   - Added `ForgotPasswordSendOtpResponse`
   - Added `ForgotPasswordResetRequest`

2. **src/main/kotlin/services/EmailService.kt**
   - Added `sendPasswordResetOtpEmail()` method

3. **src/main/kotlin/services/UserService.kt**
   - Added `sendPasswordResetOtp()` method
   - Added `resetPasswordWithOtp()` method
   - Added `maskEmail()` helper method
   - Updated constructor with `otpRepository` and `emailService`

4. **src/main/kotlin/routes/api/UserRoutes.kt**
   - Added `POST /forgot-password/send-otp` endpoint
   - Added `POST /forgot-password/reset` endpoint

5. **src/main/kotlin/plugins/Routing.kt**
   - Updated `UserService` initialization with password reset dependencies

## Testing

### Manual Testing with curl/Postman

**1. Request OTP:**
```bash
curl -X POST http://localhost:8080/api/v1/users/forgot-password/send-otp \
  -H "Content-Type: application/json" \
  -H "X-Tenant: tenant_0001" \
  -d '{
    "mobileNumber": "9876543210"
  }'
```

**2. Check Email** for OTP code

**3. Reset Password:**
```bash
curl -X POST http://localhost:8080/api/v1/users/forgot-password/reset \
  -H "Content-Type: application/json" \
  -H "X-Tenant: tenant_0001" \
  -d '{
    "mobileNumber": "9876543210",
    "otpCode": "123456",
    "newPassword": "newPassword123"
  }'
```

**4. Test Login** with new password

### Test Cases

- ✅ Valid mobile number → OTP sent to email
- ✅ Invalid mobile number → Error 404
- ✅ Valid OTP + new password → Password updated
- ✅ Invalid OTP → Error with attempts remaining
- ✅ 3 failed OTP attempts → Too many requests error
- ✅ Expired OTP (>10 min) → Invalid OTP error
- ✅ Used OTP → Invalid OTP error
- ✅ Password too short (<6 chars) → Validation error
- ✅ Email masking in response
- ✅ New password works for login

## Advantages of In-App OTP Approach

1. **Unified Experience** - User stays in Flutter app (Android/iOS/Web)
2. **No Deep Links** - No complex configuration needed
3. **Platform Consistent** - Same UX across all platforms
4. **Reuses Infrastructure** - Existing OTP table and email service
5. **More Secure** - OTP not in URL/browser history
6. **Faster Implementation** - Hours vs days for URL-based approach
7. **Mobile Optimized** - Can add SMS auto-fill on Android
8. **Better Control** - Full control over UI/UX flow

## Notes

- OTP validity: **10 minutes** (can be extended if needed)
- Max attempts: **3 attempts** per OTP
- Password minimum: **6 characters**
- Email SMTP: **Zoho** (smtp.zoho.in:587)
- Auto-cleanup: Expired OTPs removed by background job
- Multi-tenant: Works with tenant schema isolation
- BCrypt: Password hashing with automatic salt generation
