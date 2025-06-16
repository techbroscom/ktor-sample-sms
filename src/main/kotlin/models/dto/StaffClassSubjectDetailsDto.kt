package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StaffClassSubjectDetailsDto(
    val myClass: List<MyClassDto>,
    val teachingClasses: List<TeachingClassDto>
)

@Serializable
data class MyClassDto(
    val id: String,
    val name: String?
)

@Serializable
data class TeachingClassDto(
    val id: String,
    val name: String?,
    val subjectName: String?,
    val subjectId: String
)