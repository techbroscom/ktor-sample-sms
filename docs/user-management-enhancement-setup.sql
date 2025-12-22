-- User Management Enhancement Setup
-- This script adds photo_url to users table and creates user_details table

-- 1. Add photo_url column to users table
-- This should be run in each tenant schema
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);

-- 2. Create user_details table to store additional user information
-- This should be run in each tenant schema
CREATE TABLE IF NOT EXISTS user_details (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Personal Information
    date_of_birth DATE,
    gender VARCHAR(20), -- MALE, FEMALE, OTHER
    blood_group VARCHAR(10), -- A+, A-, B+, B-, AB+, AB-, O+, O-
    nationality VARCHAR(100),
    religion VARCHAR(100),

    -- Address Information
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(100),
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),

    -- Emergency Contact
    emergency_contact_name VARCHAR(100),
    emergency_contact_relationship VARCHAR(50),
    emergency_contact_mobile VARCHAR(15),
    emergency_contact_email VARCHAR(255),

    -- Parent/Guardian Information (primarily for students)
    father_name VARCHAR(100),
    father_mobile VARCHAR(15),
    father_email VARCHAR(255),
    father_occupation VARCHAR(100),

    mother_name VARCHAR(100),
    mother_mobile VARCHAR(15),
    mother_email VARCHAR(255),
    mother_occupation VARCHAR(100),

    guardian_name VARCHAR(100),
    guardian_mobile VARCHAR(15),
    guardian_email VARCHAR(255),
    guardian_relationship VARCHAR(50),
    guardian_occupation VARCHAR(100),

    -- Additional Information
    aadhar_number VARCHAR(12), -- India-specific, can be used for any national ID
    medical_conditions TEXT,
    allergies TEXT,
    special_needs TEXT,
    notes TEXT,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    -- Ensure one details record per user
    CONSTRAINT unique_user_details UNIQUE (user_id)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_user_details_user_id ON user_details(user_id);
CREATE INDEX IF NOT EXISTS idx_user_details_date_of_birth ON user_details(date_of_birth);
CREATE INDEX IF NOT EXISTS idx_user_details_gender ON user_details(gender);

-- Verify the changes
SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'users' AND column_name = 'photo_url';

SELECT column_name, data_type, character_maximum_length
FROM information_schema.columns
WHERE table_name = 'user_details'
ORDER BY ordinal_position;
