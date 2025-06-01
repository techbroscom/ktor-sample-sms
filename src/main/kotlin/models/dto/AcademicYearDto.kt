package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class AcademicYearDto(
    val id: String,
    val year: String,
    val startDate: String, // Format: YYYY-MM-DD
    val endDate: String,   // Format: YYYY-MM-DD
    val isActive: Boolean
)

@Serializable
data class CreateAcademicYearRequest(
    val year: String,
    val startDate: String, // Format: YYYY-MM-DD
    val endDate: String,   // Format: YYYY-MM-DD
    val isActive: Boolean = false
)

@Serializable
data class UpdateAcademicYearRequest(
    val year: String,
    val startDate: String, // Format: YYYY-MM-DD
    val endDate: String,   // Format: YYYY-MM-DD
    val isActive: Boolean
)

@Serializable
data class SetActiveAcademicYearRequest(
    val isActive: Boolean
)