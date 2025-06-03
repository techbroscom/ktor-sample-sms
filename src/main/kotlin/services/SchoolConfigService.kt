package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.SchoolConfigDto
import com.example.models.dto.UpdateSchoolConfigRequest
import com.example.repositories.SchoolConfigRepository
import io.ktor.http.*
import java.util.regex.Pattern

class SchoolConfigService(private val schoolConfigRepository: SchoolConfigRepository) {

    suspend fun getSchoolConfigById(id: Int): SchoolConfigDto {
        return schoolConfigRepository.findById(id)
            ?: throw ApiException("School configuration not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllSchoolConfigs(): List<SchoolConfigDto> {
        return schoolConfigRepository.findAll()
    }

    suspend fun updateSchoolConfig(id: Int, request: UpdateSchoolConfigRequest): SchoolConfigDto {

        val updated = schoolConfigRepository.update(id, request)
        if (!updated) {
            throw ApiException("School configuration not found", HttpStatusCode.NotFound)
        }

        return getSchoolConfigById(id)
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$"
        return Pattern.matches(emailPattern, email)
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Allow digits, spaces, hyphens, parentheses, and plus sign
        val phonePattern = "^[+]?[0-9\\s\\-()]+$"
        return Pattern.matches(phonePattern, phoneNumber) && phoneNumber.replace(Regex("[^0-9]"), "").length >= 10
    }

    private fun isValidUrl(url: String): Boolean {
        return try {
            val urlPattern = "^(https?://)?(www\\.)?[a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.[a-zA-Z]{2,}(/.*)?$"
            Pattern.matches(urlPattern, url)
        } catch (e: Exception) {
            false
        }
    }
}