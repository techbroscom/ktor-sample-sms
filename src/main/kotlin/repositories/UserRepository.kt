package com.example.repositories

import com.example.database.tables.Classes
import com.example.database.tables.StudentAssignments
import com.example.database.tables.UserDetails
import com.example.database.tables.UserRole
import com.example.database.tables.Users
import com.example.models.dto.CreateUserRequest
import com.example.models.dto.UpdateUserRequest
import com.example.models.dto.UserDto
import com.example.models.dto.UserWithDetailsDto
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class UserRepository {

    suspend fun create(request: CreateUserRequest, hashedPassword: String): UUID = tenantDbQuery {
        val userId = UUID.randomUUID()
        Users.insert {
            it[id] = userId
            it[email] = request.email
            it[mobileNumber] = request.mobileNumber
            it[passwordHash] = hashedPassword
            it[role] = UserRole.valueOf(request.role)
            it[firstName] = request.firstName
            it[lastName] = request.lastName
            it[photoUrl] = request.photoUrl
            it[imageUrl] = request.imageUrl
            it[imageS3Key] = request.imageS3Key
            it[createdAt] = LocalDateTime.now()
        }
        userId
    }

    suspend fun findById(id: UUID): UserDto? = tenantDbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findByEmail(email: String): List<UserDto>? = tenantDbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .map { mapRowToDto(it) }
    }

    suspend fun findByAdminType(): List<UserDto>? = tenantDbQuery {
        Users.selectAll()
            .where { Users.role eq UserRole.ADMIN }
            .map { mapRowToDto(it) }
    }

    suspend fun findByMobile(mobile: String): List<UserDto>? = tenantDbQuery {
        Users.selectAll()
            .where { Users.mobileNumber eq mobile }
            .map { mapRowToDto(it) }
    }

    suspend fun findPasswordHashByEmail(email: String): String? = tenantDbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .map { it[Users.passwordHash] }
            .lastOrNull()
    }

    suspend fun findPasswordHashByMobile(mobile: String): String? = tenantDbQuery {
        Users.selectAll()
            .where { Users.mobileNumber eq mobile }
            .map { it[Users.passwordHash] }
            .lastOrNull()
    }

    suspend fun findAll(): List<UserDto> = tenantDbQuery {
        Users.selectAll()
            .orderBy(Users.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByRole(role: UserRole): List<UserDto> = tenantDbQuery {
        Users.selectAll()
            .where { Users.role eq role }
            .orderBy(Users.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: UUID, request: UpdateUserRequest): Boolean = tenantDbQuery {
        Users.update({ Users.id eq id }) {
            it[email] = request.email
            it[mobileNumber] = request.mobileNumber
            it[role] = UserRole.valueOf(request.role)
            it[firstName] = request.firstName
            it[lastName] = request.lastName
            it[photoUrl] = request.photoUrl
            it[imageUrl] = request.imageUrl
            it[imageS3Key] = request.imageS3Key
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun updatePassword(id: UUID, hashedPassword: String): Boolean = tenantDbQuery {
        Users.update({ Users.id eq id }) {
            it[passwordHash] = hashedPassword
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: UUID): Boolean = tenantDbQuery {
        Users.deleteWhere { Users.id eq id } > 0
    }

    suspend fun emailExists(email: String): Boolean = tenantDbQuery {
        Users.selectAll()
            .where { Users.email eq email }
            .count() > 0
    }

    suspend fun emailExistsForOtherUser(email: String, userId: UUID): Boolean = tenantDbQuery {
        Users.selectAll()
            .where { (Users.email eq email) and (Users.id neq userId) }
            .count() > 0
    }

    suspend fun countUsersWithFilters(
        role: String? = null,
        classId: String? = null,
        search: String? = null
    ): Long = tenantDbQuery {
        val query = Users
            .leftJoin(StudentAssignments, { Users.id }, { StudentAssignments.studentId })
            .leftJoin(Classes, { StudentAssignments.classId }, { Classes.id })
            .select(Users.id)
            .withDistinct()

        // Apply role filter
        role?.let {
            try {
                val userRole = UserRole.valueOf(it.uppercase())
                query.andWhere { Users.role eq userRole }
            } catch (e: IllegalArgumentException) {
                return@tenantDbQuery 0
            }
        }

        // Apply class filter
        classId?.let {
            try {
                val classUuid = UUID.fromString(it)
                query.andWhere { Classes.id eq classUuid }
            } catch (e: IllegalArgumentException) {
                return@tenantDbQuery 0
            }
        }

        // Apply search filter
        search?.let { searchTerm ->
            if (searchTerm.isNotBlank()) {
                val searchPattern = "%${searchTerm.lowercase()}%"
                query.andWhere {
                    (Users.firstName.lowerCase() like searchPattern) or
                    (Users.lastName.lowerCase() like searchPattern) or
                    (Users.email.lowerCase() like searchPattern) or
                    (Users.mobileNumber like searchPattern)
                }
            }
        }

        query.count()
    }

    suspend fun findUsersWithFilters(
        role: String? = null,
        classId: String? = null,
        search: String? = null,
        page: Int = 1,
        pageSize: Int = 20
    ): List<UserWithDetailsDto> = tenantDbQuery {
        val offset = (page - 1) * pageSize

        val query = Users
            .leftJoin(UserDetails, { Users.id }, { UserDetails.userId })
            .leftJoin(StudentAssignments, { Users.id }, { StudentAssignments.studentId })
            .leftJoin(Classes, { StudentAssignments.classId }, { Classes.id })
            .selectAll()

        // Apply role filter
        role?.let {
            try {
                val userRole = UserRole.valueOf(it.uppercase())
                query.andWhere { Users.role eq userRole }
            } catch (e: IllegalArgumentException) {
                // Invalid role, return empty list
                return@tenantDbQuery emptyList()
            }
        }

        // Apply class filter
        classId?.let {
            try {
                val classUuid = UUID.fromString(it)
                query.andWhere { Classes.id eq classUuid }
            } catch (e: IllegalArgumentException) {
                // Invalid UUID, return empty list
                return@tenantDbQuery emptyList()
            }
        }

        // Apply search filter (search in firstName, lastName, email, mobileNumber)
        search?.let { searchTerm ->
            if (searchTerm.isNotBlank()) {
                val searchPattern = "%${searchTerm.lowercase()}%"
                query.andWhere {
                    (Users.firstName.lowerCase() like searchPattern) or
                    (Users.lastName.lowerCase() like searchPattern) or
                    (Users.email.lowerCase() like searchPattern) or
                    (Users.mobileNumber like searchPattern)
                }
            }
        }

        // Group results to handle multiple class assignments and apply pagination
        query.orderBy(Users.createdAt to SortOrder.DESC)
            .groupBy { it[Users.id] }
            .values
            .drop(offset)
            .take(pageSize)
            .map { rows ->
                val firstRow = rows.first()
                val user = mapRowToDto(firstRow)
                val details = if (firstRow.getOrNull(UserDetails.id) != null) {
                    mapRowToUserDetailsDto(firstRow)
                } else null

                val className = firstRow.getOrNull(Classes.className)
                val sectionName = firstRow.getOrNull(Classes.sectionName)

                UserWithDetailsDto(
                    user = user,
                    details = details,
                    className = className,
                    sectionName = sectionName
                )
            }
    }

    private fun mapRowToDto(row: ResultRow): UserDto {
        return UserDto(
            id = row[Users.id].toString(),
            email = row[Users.email],
            mobileNumber = row[Users.mobileNumber],
            role = row[Users.role].name,
            firstName = row[Users.firstName],
            lastName = row[Users.lastName],
            photoUrl = row[Users.photoUrl],
            imageUrl = row[Users.imageUrl],
            imageS3Key = row[Users.imageS3Key],
            createdAt = row[Users.createdAt].toString(),
            updatedAt = row[Users.updatedAt]?.toString()
        )
    }

    private fun mapRowToUserDetailsDto(row: ResultRow): com.example.models.dto.UserDetailsDto {
        return com.example.models.dto.UserDetailsDto(
            id = row[UserDetails.id].toString(),
            userId = row[UserDetails.userId].toString(),
            dateOfBirth = row[UserDetails.dateOfBirth]?.toString(),
            gender = row[UserDetails.gender],
            bloodGroup = row[UserDetails.bloodGroup],
            nationality = row[UserDetails.nationality],
            religion = row[UserDetails.religion],
            addressLine1 = row[UserDetails.addressLine1],
            addressLine2 = row[UserDetails.addressLine2],
            city = row[UserDetails.city],
            state = row[UserDetails.state],
            postalCode = row[UserDetails.postalCode],
            country = row[UserDetails.country],
            emergencyContactName = row[UserDetails.emergencyContactName],
            emergencyContactRelationship = row[UserDetails.emergencyContactRelationship],
            emergencyContactMobile = row[UserDetails.emergencyContactMobile],
            emergencyContactEmail = row[UserDetails.emergencyContactEmail],
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
            aadharNumber = row[UserDetails.aadharNumber],
            medicalConditions = row[UserDetails.medicalConditions],
            allergies = row[UserDetails.allergies],
            specialNeeds = row[UserDetails.specialNeeds],
            notes = row[UserDetails.notes],
            createdAt = row[UserDetails.createdAt].toString(),
            updatedAt = row[UserDetails.updatedAt]?.toString()
        )
    }
}