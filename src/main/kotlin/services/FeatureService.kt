package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.CreateFeatureRequest
import com.example.models.dto.FeatureDto
import com.example.models.dto.UpdateFeatureRequest
import com.example.repositories.FeatureRepository
import io.ktor.http.*

class FeatureService(
    private val featureRepository: FeatureRepository
) {

    suspend fun createFeature(request: CreateFeatureRequest): FeatureDto {
        // Validate feature key uniqueness
        if (featureRepository.exists(request.featureKey)) {
            throw ApiException("Feature with key '${request.featureKey}' already exists", HttpStatusCode.Conflict)
        }

        // Validate limit configuration
        if (request.hasLimit) {
            if (request.limitType.isNullOrBlank() || request.limitValue == null || request.limitUnit.isNullOrBlank()) {
                throw ApiException("limitType, limitValue, and limitUnit are required when hasLimit is true", HttpStatusCode.BadRequest)
            }
        }

        return featureRepository.create(request)
    }

    suspend fun getFeatureById(id: Int): FeatureDto {
        return featureRepository.findById(id)
            ?: throw ApiException("Feature not found", HttpStatusCode.NotFound)
    }

    suspend fun getFeatureByKey(featureKey: String): FeatureDto {
        return featureRepository.findByFeatureKey(featureKey)
            ?: throw ApiException("Feature not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllFeatures(activeOnly: Boolean = false): List<FeatureDto> {
        return featureRepository.findAll(activeOnly)
    }

    suspend fun getFeaturesByCategory(category: String): List<FeatureDto> {
        return featureRepository.findByCategory(category)
    }

    suspend fun updateFeature(id: Int, request: UpdateFeatureRequest): FeatureDto {
        if (!featureRepository.existsById(id)) {
            throw ApiException("Feature not found", HttpStatusCode.NotFound)
        }

        // Validate limit configuration if being updated
        if (request.hasLimit == true) {
            if (request.limitType.isNullOrBlank() || request.limitValue == null || request.limitUnit.isNullOrBlank()) {
                throw ApiException("limitType, limitValue, and limitUnit are required when hasLimit is true", HttpStatusCode.BadRequest)
            }
        }

        val updated = featureRepository.update(id, request)
        if (!updated) {
            throw ApiException("Failed to update feature", HttpStatusCode.InternalServerError)
        }

        return getFeatureById(id)
    }

    suspend fun deleteFeature(id: Int): Boolean {
        if (!featureRepository.existsById(id)) {
            throw ApiException("Feature not found", HttpStatusCode.NotFound)
        }

        return featureRepository.delete(id)
    }
}
