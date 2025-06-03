package com.example.services


import com.example.exceptions.ApiException
import com.example.models.dto.CreateRulesAndRegulationsRequest
import com.example.models.dto.RulesAndRegulationsDto
import com.example.models.dto.UpdateRulesAndRegulationsRequest
import com.example.repositories.RulesAndRegulationsRepository
import io.ktor.http.*
import java.time.LocalDate
import java.time.format.DateTimeParseException

class RulesAndRegulationsService(private val rulesAndRegulationsRepository: RulesAndRegulationsRepository) {

    suspend fun createRule(request: CreateRulesAndRegulationsRequest): RulesAndRegulationsDto {
        validateRuleRequest(request.rule)

        val ruleId = rulesAndRegulationsRepository.create(request)
        return getRuleById(ruleId)
    }

    suspend fun getRuleById(id: Int): RulesAndRegulationsDto {
        return rulesAndRegulationsRepository.findById(id)
            ?: throw ApiException("Rule not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllRules(): List<RulesAndRegulationsDto> {
        return rulesAndRegulationsRepository.findAll()
    }

    suspend fun updateRule(id: Int, request: UpdateRulesAndRegulationsRequest): RulesAndRegulationsDto {
        validateRuleRequest(request.rule)

        val updated = rulesAndRegulationsRepository.update(id, request)
        if (!updated) {
            throw ApiException("Rule not found", HttpStatusCode.NotFound)
        }

        return getRuleById(id)
    }

    suspend fun deleteRule(id: Int) {
        val deleted = rulesAndRegulationsRepository.delete(id)
        if (!deleted) {
            throw ApiException("Rule not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun searchRules(keyword: String): List<RulesAndRegulationsDto> {
        if (keyword.isBlank()) {
            throw ApiException("Search keyword cannot be empty", HttpStatusCode.BadRequest)
        }

        if (keyword.length < 2) {
            throw ApiException("Search keyword must be at least 2 characters long", HttpStatusCode.BadRequest)
        }

        return rulesAndRegulationsRepository.searchByKeyword(keyword)
    }

    suspend fun getRecentRules(limit: Int = 10): List<RulesAndRegulationsDto> {
        if (limit <= 0 || limit > 100) {
            throw ApiException("Limit must be between 1 and 100", HttpStatusCode.BadRequest)
        }

        return rulesAndRegulationsRepository.findRecent(limit)
    }

    suspend fun getRulesByDateRange(startDate: String, endDate: String): List<RulesAndRegulationsDto> {
        validateDateFormat(startDate, "Start date")
        validateDateFormat(endDate, "End date")

        val start = LocalDate.parse(startDate)
        val end = LocalDate.parse(endDate)

        if (start.isAfter(end)) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        return rulesAndRegulationsRepository.findByDateRange(startDate, endDate)
    }

    suspend fun getRulesCount(): Long {
        return rulesAndRegulationsRepository.getTotalCount()
    }

    private fun validateRuleRequest(rule: String) {
        when {
            rule.isBlank() -> throw ApiException("Rule cannot be empty", HttpStatusCode.BadRequest)
            rule.length < 10 -> throw ApiException("Rule must be at least 10 characters long", HttpStatusCode.BadRequest)
            rule.length > 10000 -> throw ApiException("Rule is too long (max 10000 characters)", HttpStatusCode.BadRequest)
        }

        // Check for inappropriate content patterns (basic validation)
        val inappropriatePatterns = listOf(
            "\\b(test|dummy|sample)\\b".toRegex(RegexOption.IGNORE_CASE)
        )

        inappropriatePatterns.forEach { pattern ->
            if (pattern.containsMatchIn(rule)) {
                throw ApiException("Rule contains inappropriate or placeholder content", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun validateDateFormat(date: String, fieldName: String) {
        try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw ApiException("$fieldName must be in format YYYY-MM-DD", HttpStatusCode.BadRequest)
        }
    }
}