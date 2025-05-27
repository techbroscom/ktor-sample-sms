package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class HolidayDto(
    val id: Int? = null,
    val name: String,
    val date: String, // Format: YYYY-MM-DD
    val description: String? = null,
    val isPublicHoliday: Boolean
)

@Serializable
data class CreateHolidayRequest(
    val name: String,
    val date: String, // Format: YYYY-MM-DD
    val description: String? = null,
    val isPublicHoliday: Boolean
)

@Serializable
data class UpdateHolidayRequest(
    val name: String,
    val date: String, // Format: YYYY-MM-DD
    val description: String? = null,
    val isPublicHoliday: Boolean
)