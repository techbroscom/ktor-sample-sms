package com.example.repositories

import com.example.database.tables.UserDetails
import com.example.models.dto.CreateUserDetailsRequest
import com.example.models.dto.UpdateUserDetailsRequest
import com.example.models.dto.UserDetailsDto
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class UserDetailsRepository {

    suspend fun create(request: CreateUserDetailsRequest): UUID = tenantDbQuery {
        val detailsId = UUID.randomUUID()
        val userId = UUID.fromString(request.userId)

        UserDetails.insert {
            it[id] = detailsId
            it[UserDetails.userId] = userId

            // Personal Information
            it[dateOfBirth] = request.dateOfBirth?.let { dob -> LocalDate.parse(dob) }
            it[gender] = request.gender
            it[bloodGroup] = request.bloodGroup
            it[nationality] = request.nationality
            it[religion] = request.religion

            // Address Information
            it[addressLine1] = request.addressLine1
            it[addressLine2] = request.addressLine2
            it[city] = request.city
            it[state] = request.state
            it[postalCode] = request.postalCode
            it[country] = request.country

            // Emergency Contact
            it[emergencyContactName] = request.emergencyContactName
            it[emergencyContactRelationship] = request.emergencyContactRelationship
            it[emergencyContactMobile] = request.emergencyContactMobile
            it[emergencyContactEmail] = request.emergencyContactEmail

            // Parent/Guardian Information
            it[fatherName] = request.fatherName
            it[fatherMobile] = request.fatherMobile
            it[fatherEmail] = request.fatherEmail
            it[fatherOccupation] = request.fatherOccupation

            it[motherName] = request.motherName
            it[motherMobile] = request.motherMobile
            it[motherEmail] = request.motherEmail
            it[motherOccupation] = request.motherOccupation

            it[guardianName] = request.guardianName
            it[guardianMobile] = request.guardianMobile
            it[guardianEmail] = request.guardianEmail
            it[guardianRelationship] = request.guardianRelationship
            it[guardianOccupation] = request.guardianOccupation

            // Additional Information
            it[aadharNumber] = request.aadharNumber
            it[medicalConditions] = request.medicalConditions
            it[allergies] = request.allergies
            it[specialNeeds] = request.specialNeeds
            it[notes] = request.notes

            it[createdAt] = LocalDateTime.now()
        }
        detailsId
    }

    suspend fun findById(id: UUID): UserDetailsDto? = tenantDbQuery {
        UserDetails.selectAll()
            .where { UserDetails.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByUserId(userId: UUID): UserDetailsDto? = tenantDbQuery {
        UserDetails.selectAll()
            .where { UserDetails.userId eq userId }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun update(userId: UUID, request: UpdateUserDetailsRequest): Boolean = tenantDbQuery {
        UserDetails.update({ UserDetails.userId eq userId }) {
            // Personal Information
            request.dateOfBirth?.let { dob -> it[dateOfBirth] = LocalDate.parse(dob) }
            request.gender?.let { g -> it[gender] = g }
            request.bloodGroup?.let { bg -> it[bloodGroup] = bg }
            request.nationality?.let { n -> it[nationality] = n }
            request.religion?.let { r -> it[religion] = r }

            // Address Information
            request.addressLine1?.let { a -> it[addressLine1] = a }
            request.addressLine2?.let { a -> it[addressLine2] = a }
            request.city?.let { c -> it[city] = c }
            request.state?.let { s -> it[state] = s }
            request.postalCode?.let { p -> it[postalCode] = p }
            request.country?.let { c -> it[country] = c }

            // Emergency Contact
            request.emergencyContactName?.let { n -> it[emergencyContactName] = n }
            request.emergencyContactRelationship?.let { r -> it[emergencyContactRelationship] = r }
            request.emergencyContactMobile?.let { m -> it[emergencyContactMobile] = m }
            request.emergencyContactEmail?.let { e -> it[emergencyContactEmail] = e }

            // Parent/Guardian Information
            request.fatherName?.let { n -> it[fatherName] = n }
            request.fatherMobile?.let { m -> it[fatherMobile] = m }
            request.fatherEmail?.let { e -> it[fatherEmail] = e }
            request.fatherOccupation?.let { o -> it[fatherOccupation] = o }

            request.motherName?.let { n -> it[motherName] = n }
            request.motherMobile?.let { m -> it[motherMobile] = m }
            request.motherEmail?.let { e -> it[motherEmail] = e }
            request.motherOccupation?.let { o -> it[motherOccupation] = o }

            request.guardianName?.let { n -> it[guardianName] = n }
            request.guardianMobile?.let { m -> it[guardianMobile] = m }
            request.guardianEmail?.let { e -> it[guardianEmail] = e }
            request.guardianRelationship?.let { r -> it[guardianRelationship] = r }
            request.guardianOccupation?.let { o -> it[guardianOccupation] = o }

            // Additional Information
            request.aadharNumber?.let { a -> it[aadharNumber] = a }
            request.medicalConditions?.let { m -> it[medicalConditions] = m }
            request.allergies?.let { a -> it[allergies] = a }
            request.specialNeeds?.let { s -> it[specialNeeds] = s }
            request.notes?.let { n -> it[notes] = n }

            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(userId: UUID): Boolean = tenantDbQuery {
        UserDetails.deleteWhere { UserDetails.userId eq userId } > 0
    }

    suspend fun existsForUser(userId: UUID): Boolean = tenantDbQuery {
        UserDetails.selectAll()
            .where { UserDetails.userId eq userId }
            .count() > 0
    }

    private fun mapRowToDto(row: ResultRow): UserDetailsDto {
        return UserDetailsDto(
            id = row[UserDetails.id].toString(),
            userId = row[UserDetails.userId].toString(),

            // Personal Information
            dateOfBirth = row[UserDetails.dateOfBirth]?.toString(),
            gender = row[UserDetails.gender],
            bloodGroup = row[UserDetails.bloodGroup],
            nationality = row[UserDetails.nationality],
            religion = row[UserDetails.religion],

            // Address Information
            addressLine1 = row[UserDetails.addressLine1],
            addressLine2 = row[UserDetails.addressLine2],
            city = row[UserDetails.city],
            state = row[UserDetails.state],
            postalCode = row[UserDetails.postalCode],
            country = row[UserDetails.country],

            // Emergency Contact
            emergencyContactName = row[UserDetails.emergencyContactName],
            emergencyContactRelationship = row[UserDetails.emergencyContactRelationship],
            emergencyContactMobile = row[UserDetails.emergencyContactMobile],
            emergencyContactEmail = row[UserDetails.emergencyContactEmail],

            // Parent/Guardian Information
            fatherName = row[UserDetails.fatherName],
            fatherMobile = row[UserDetails.fatherMobile],
            fatherEmail = row[UserDetails.fatherEmail],
            fatherOccupation = row[UserDetails.fatherOccupation],

            motherName = row[UserDetails.motherName],
            motherMobile = row[UserDetails.motherMobile],
            motherEmail = row[UserDetails.motherEmail],
            motherOccupation = row[UserDetails.motherOccupation],

            guardianName = row[UserDetails.guardianName],
            guardianMobile = row[UserDetails.guardianMobile],
            guardianEmail = row[UserDetails.guardianEmail],
            guardianRelationship = row[UserDetails.guardianRelationship],
            guardianOccupation = row[UserDetails.guardianOccupation],

            // Additional Information
            aadharNumber = row[UserDetails.aadharNumber],
            medicalConditions = row[UserDetails.medicalConditions],
            allergies = row[UserDetails.allergies],
            specialNeeds = row[UserDetails.specialNeeds],
            notes = row[UserDetails.notes],

            // Timestamps
            createdAt = row[UserDetails.createdAt].toString(),
            updatedAt = row[UserDetails.updatedAt]?.toString()
        )
    }
}
