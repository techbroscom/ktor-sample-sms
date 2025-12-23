package com.example.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object UserDetails : Table("user_details") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").references(Users.id, onDelete = ReferenceOption.CASCADE)

    // Personal Information
    val dateOfBirth = date("date_of_birth").nullable()
    val gender = varchar("gender", 20).nullable()
    val bloodGroup = varchar("blood_group", 10).nullable()
    val nationality = varchar("nationality", 100).nullable()
    val religion = varchar("religion", 100).nullable()

    // Address Information
    val addressLine1 = varchar("address_line1", 255).nullable()
    val addressLine2 = varchar("address_line2", 255).nullable()
    val city = varchar("city", 100).nullable()
    val state = varchar("state", 100).nullable()
    val postalCode = varchar("postal_code", 20).nullable()
    val country = varchar("country", 100).nullable()

    // Emergency Contact
    val emergencyContactName = varchar("emergency_contact_name", 100).nullable()
    val emergencyContactRelationship = varchar("emergency_contact_relationship", 50).nullable()
    val emergencyContactMobile = varchar("emergency_contact_mobile", 15).nullable()
    val emergencyContactEmail = varchar("emergency_contact_email", 255).nullable()

    // Parent/Guardian Information
    val fatherName = varchar("father_name", 100).nullable()
    val fatherMobile = varchar("father_mobile", 15).nullable()
    val fatherEmail = varchar("father_email", 255).nullable()
    val fatherOccupation = varchar("father_occupation", 100).nullable()

    val motherName = varchar("mother_name", 100).nullable()
    val motherMobile = varchar("mother_mobile", 15).nullable()
    val motherEmail = varchar("mother_email", 255).nullable()
    val motherOccupation = varchar("mother_occupation", 100).nullable()

    val guardianName = varchar("guardian_name", 100).nullable()
    val guardianMobile = varchar("guardian_mobile", 15).nullable()
    val guardianEmail = varchar("guardian_email", 255).nullable()
    val guardianRelationship = varchar("guardian_relationship", 50).nullable()
    val guardianOccupation = varchar("guardian_occupation", 100).nullable()

    // Additional Information
    val aadharNumber = varchar("aadhar_number", 12).nullable()
    val medicalConditions = text("medical_conditions").nullable()
    val allergies = text("allergies").nullable()
    val specialNeeds = text("special_needs").nullable()
    val notes = text("notes").nullable()

    // Timestamps
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(userId)
    }
}
