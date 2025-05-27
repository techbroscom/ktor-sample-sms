package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreateUserRequest
import com.example.models.dto.UpdateUserRequest
import com.example.models.dto.UserDto
import com.example.repositories.UserRepository
import io.ktor.http.*

class UserService(private val userRepository: UserRepository) {

    suspend fun createUser(request: CreateUserRequest): UserDto {
        validateUserRequest(request.name, request.age)

        val userId = userRepository.create(request)
        return userRepository.findById(userId)
            ?: throw ApiException("Failed to create user", HttpStatusCode.InternalServerError)
    }

    suspend fun getUserById(id: Int): UserDto {
        return userRepository.findById(id)
            ?: throw ApiException("User not found", HttpStatusCode.NotFound)
    }

    suspend fun updateUser(id: Int, request: UpdateUserRequest): UserDto {
        validateUserRequest(request.name, request.age)

        val updated = userRepository.update(id, request)
        if (!updated) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }

        return getUserById(id)
    }

    suspend fun deleteUser(id: Int) {
        val deleted = userRepository.delete(id)
        if (!deleted) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getAllUsers(): List<UserDto> {
        return userRepository.findAll()
    }

    private fun validateUserRequest(name: String, age: Int) {
        when {
            name.isBlank() -> throw ApiException("Name cannot be empty", HttpStatusCode.BadRequest)
            age < 0 -> throw ApiException("Age cannot be negative", HttpStatusCode.BadRequest)
            age > 150 -> throw ApiException("Age must be realistic", HttpStatusCode.BadRequest)
        }
    }
}