package com.example.repositories

import com.example.database.tables.Users
import com.example.models.dto.CreateUserRequest
import com.example.models.dto.UpdateUserRequest
import com.example.models.dto.UserDto
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

class UserRepository {

    suspend fun create(request: CreateUserRequest): Int = dbQuery {
        Users.insert {
            it[name] = request.name
            it[age] = request.age
        }[Users.id]
    }

    suspend fun findById(id: Int): UserDto? = dbQuery {
        Users.selectAll()
            .where { Users.id eq id }
            .map {
                UserDto(
                    id = it[Users.id],
                    name = it[Users.name],
                    age = it[Users.age]
                )
            }
            .singleOrNull()
    }

    suspend fun update(id: Int, request: UpdateUserRequest): Boolean = dbQuery {
        Users.update({ Users.id eq id }) {
            it[name] = request.name
            it[age] = request.age
        } > 0
    }

    suspend fun delete(id: Int): Boolean = dbQuery {
        Users.deleteWhere { Users.id.eq(id) } > 0
    }

    suspend fun findAll(): List<UserDto> = dbQuery {
        Users.selectAll()
            .map {
                UserDto(
                    id = it[Users.id],
                    name = it[Users.name],
                    age = it[Users.age]
                )
            }
    }
}