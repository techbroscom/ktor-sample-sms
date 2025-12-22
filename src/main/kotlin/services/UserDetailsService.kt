package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreateUserDetailsRequest
import com.example.models.dto.UpdateUserDetailsRequest
import com.example.models.dto.UserDetailsDto
import com.example.repositories.UserDetailsRepository
import com.example.repositories.UserRepository
import io.ktor.http.*
import java.util.*

class UserDetailsService(
    private val userDetailsRepository: UserDetailsRepository,
    private val userRepository: UserRepository
) {

    suspend fun createUserDetails(request: CreateUserDetailsRequest): UserDetailsDto {
        // Validate user exists
        val userId = try {
            UUID.fromString(request.userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        val userExists = userRepository.findById(userId) != null
        if (!userExists) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }

        // Check if user details already exist
        if (userDetailsRepository.existsForUser(userId)) {
            throw ApiException("User details already exist for this user", HttpStatusCode.Conflict)
        }

        validateUserDetailsRequest(request)

        val detailsId = userDetailsRepository.create(request)
        return getUserDetailsById(detailsId)
    }

    suspend fun getUserDetailsById(id: UUID): UserDetailsDto {
        return userDetailsRepository.findById(id)
            ?: throw ApiException("User details not found", HttpStatusCode.NotFound)
    }

    suspend fun getUserDetailsByUserId(userId: String): UserDetailsDto {
        val uuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        return userDetailsRepository.findByUserId(uuid)
            ?: throw ApiException("User details not found for this user", HttpStatusCode.NotFound)
    }

    suspend fun updateUserDetails(userId: String, request: UpdateUserDetailsRequest): UserDetailsDto {
        val uuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        // Validate user exists
        val userExists = userRepository.findById(uuid) != null
        if (!userExists) {
            throw ApiException("User not found", HttpStatusCode.NotFound)
        }

        // Check if user details exist
        if (!userDetailsRepository.existsForUser(uuid)) {
            throw ApiException("User details not found for this user", HttpStatusCode.NotFound)
        }

        validateUpdateUserDetailsRequest(request)

        val updated = userDetailsRepository.update(uuid, request)
        if (!updated) {
            throw ApiException("Failed to update user details", HttpStatusCode.InternalServerError)
        }

        return userDetailsRepository.findByUserId(uuid)
            ?: throw ApiException("User details not found after update", HttpStatusCode.InternalServerError)
    }

    suspend fun deleteUserDetails(userId: String) {
        val uuid = try {
            UUID.fromString(userId)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid user ID format", HttpStatusCode.BadRequest)
        }

        val deleted = userDetailsRepository.delete(uuid)
        if (!deleted) {
            throw ApiException("User details not found", HttpStatusCode.NotFound)
        }
    }

    private fun validateUserDetailsRequest(request: CreateUserDetailsRequest) {
        // Validate date of birth format if provided
        request.dateOfBirth?.let { dob ->
            try {
                java.time.LocalDate.parse(dob)
            } catch (e: Exception) {
                throw ApiException("Invalid date of birth format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
            }
        }

        // Validate gender if provided
        request.gender?.let { gender ->
            if (gender.isNotBlank() && gender.uppercase() !in listOf("MALE", "FEMALE", "OTHER")) {
                throw ApiException("Invalid gender. Must be MALE, FEMALE, or OTHER", HttpStatusCode.BadRequest)
            }
        }

        // Validate blood group if provided
        request.bloodGroup?.let { bg ->
            val validBloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
            if (bg.isNotBlank() && bg !in validBloodGroups) {
                throw ApiException("Invalid blood group. Must be one of: ${validBloodGroups.joinToString(", ")}", HttpStatusCode.BadRequest)
            }
        }

        // Validate email formats
        request.emergencyContactEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid emergency contact email format", HttpStatusCode.BadRequest)
            }
        }

        request.fatherEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid father email format", HttpStatusCode.BadRequest)
            }
        }

        request.motherEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid mother email format", HttpStatusCode.BadRequest)
            }
        }

        request.guardianEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid guardian email format", HttpStatusCode.BadRequest)
            }
        }

        // Validate aadhar number if provided (should be 12 digits)
        request.aadharNumber?.let { aadhar ->
            if (aadhar.isNotBlank() && !aadhar.matches(Regex("^\\d{12}$"))) {
                throw ApiException("Invalid Aadhar number. Must be 12 digits", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun validateUpdateUserDetailsRequest(request: UpdateUserDetailsRequest) {
        // Validate date of birth format if provided
        request.dateOfBirth?.let { dob ->
            try {
                java.time.LocalDate.parse(dob)
            } catch (e: Exception) {
                throw ApiException("Invalid date of birth format. Use YYYY-MM-DD", HttpStatusCode.BadRequest)
            }
        }

        // Validate gender if provided
        request.gender?.let { gender ->
            if (gender.isNotBlank() && gender.uppercase() !in listOf("MALE", "FEMALE", "OTHER")) {
                throw ApiException("Invalid gender. Must be MALE, FEMALE, or OTHER", HttpStatusCode.BadRequest)
            }
        }

        // Validate blood group if provided
        request.bloodGroup?.let { bg ->
            val validBloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
            if (bg.isNotBlank() && bg !in validBloodGroups) {
                throw ApiException("Invalid blood group. Must be one of: ${validBloodGroups.joinToString(", ")}", HttpStatusCode.BadRequest)
            }
        }

        // Validate email formats
        request.emergencyContactEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid emergency contact email format", HttpStatusCode.BadRequest)
            }
        }

        request.fatherEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid father email format", HttpStatusCode.BadRequest)
            }
        }

        request.motherEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid mother email format", HttpStatusCode.BadRequest)
            }
        }

        request.guardianEmail?.let { email ->
            if (email.isNotBlank() && !isValidEmail(email)) {
                throw ApiException("Invalid guardian email format", HttpStatusCode.BadRequest)
            }
        }

        // Validate aadhar number if provided (should be 12 digits)
        request.aadharNumber?.let { aadhar ->
            if (aadhar.isNotBlank() && !aadhar.matches(Regex("^\\d{12}$"))) {
                throw ApiException("Invalid Aadhar number. Must be 12 digits", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}
