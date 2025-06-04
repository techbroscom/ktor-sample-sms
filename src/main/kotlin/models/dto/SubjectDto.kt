package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SubjectDto(
    val id: String? = null,
    val name: String,
    val code: String
)

@Serializable
data class CreateSubjectRequest(
    val name: String,
    val code: String
)

@Serializable
data class UpdateSubjectRequest(
    val name: String,
    val code: String
)