package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassDto(
    val id: String? = null,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String? = null // Optional for joined queries
)

@Serializable
data class CreateClassRequest(
    val className: String,
    val sectionName: String,
    val academicYearId: String
)

@Serializable
data class UpdateClassRequest(
    val className: String,
    val sectionName: String,
    val academicYearId: String
)