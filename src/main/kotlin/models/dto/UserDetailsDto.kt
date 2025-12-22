package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDetailsDto(
    val id: String,
    val userId: String,

    // Personal Information
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val bloodGroup: String? = null,
    val nationality: String? = null,
    val religion: String? = null,

    // Address Information
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,

    // Emergency Contact
    val emergencyContactName: String? = null,
    val emergencyContactRelationship: String? = null,
    val emergencyContactMobile: String? = null,
    val emergencyContactEmail: String? = null,

    // Parent/Guardian Information
    val fatherName: String? = null,
    val fatherMobile: String? = null,
    val fatherEmail: String? = null,
    val fatherOccupation: String? = null,

    val motherName: String? = null,
    val motherMobile: String? = null,
    val motherEmail: String? = null,
    val motherOccupation: String? = null,

    val guardianName: String? = null,
    val guardianMobile: String? = null,
    val guardianEmail: String? = null,
    val guardianRelationship: String? = null,
    val guardianOccupation: String? = null,

    // Additional Information
    val aadharNumber: String? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,
    val specialNeeds: String? = null,
    val notes: String? = null,

    // Timestamps
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateUserDetailsRequest(
    val userId: String,

    // Personal Information
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val bloodGroup: String? = null,
    val nationality: String? = null,
    val religion: String? = null,

    // Address Information
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,

    // Emergency Contact
    val emergencyContactName: String? = null,
    val emergencyContactRelationship: String? = null,
    val emergencyContactMobile: String? = null,
    val emergencyContactEmail: String? = null,

    // Parent/Guardian Information
    val fatherName: String? = null,
    val fatherMobile: String? = null,
    val fatherEmail: String? = null,
    val fatherOccupation: String? = null,

    val motherName: String? = null,
    val motherMobile: String? = null,
    val motherEmail: String? = null,
    val motherOccupation: String? = null,

    val guardianName: String? = null,
    val guardianMobile: String? = null,
    val guardianEmail: String? = null,
    val guardianRelationship: String? = null,
    val guardianOccupation: String? = null,

    // Additional Information
    val aadharNumber: String? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,
    val specialNeeds: String? = null,
    val notes: String? = null
)

@Serializable
data class UpdateUserDetailsRequest(
    // Personal Information
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val bloodGroup: String? = null,
    val nationality: String? = null,
    val religion: String? = null,

    // Address Information
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val postalCode: String? = null,
    val country: String? = null,

    // Emergency Contact
    val emergencyContactName: String? = null,
    val emergencyContactRelationship: String? = null,
    val emergencyContactMobile: String? = null,
    val emergencyContactEmail: String? = null,

    // Parent/Guardian Information
    val fatherName: String? = null,
    val fatherMobile: String? = null,
    val fatherEmail: String? = null,
    val fatherOccupation: String? = null,

    val motherName: String? = null,
    val motherMobile: String? = null,
    val motherEmail: String? = null,
    val motherOccupation: String? = null,

    val guardianName: String? = null,
    val guardianMobile: String? = null,
    val guardianEmail: String? = null,
    val guardianRelationship: String? = null,
    val guardianOccupation: String? = null,

    // Additional Information
    val aadharNumber: String? = null,
    val medicalConditions: String? = null,
    val allergies: String? = null,
    val specialNeeds: String? = null,
    val notes: String? = null
)

// Enhanced user DTO with details
@Serializable
data class UserWithDetailsDto(
    val user: UserDto,
    val details: UserDetailsDto? = null,
    val className: String? = null,
    val sectionName: String? = null
)
