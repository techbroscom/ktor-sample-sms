package com.example.repositories

import com.example.database.tables.UserRole
import com.example.database.tables.Users
import com.example.models.dto.CreateUserRequest
import com.example.models.dto.UpdateUserRequest
import com.example.models.dto.UserDto
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

    private fun mapRowToDto(row: ResultRow): UserDto {
        return UserDto(
            id = row[Users.id].toString(),
            email = row[Users.email],
            mobileNumber = row[Users.mobileNumber],
            role = row[Users.role].name,
            firstName = row[Users.firstName],
            lastName = row[Users.lastName],
            createdAt = row[Users.createdAt].toString(),
            updatedAt = row[Users.updatedAt]?.toString()
        )
    }
}