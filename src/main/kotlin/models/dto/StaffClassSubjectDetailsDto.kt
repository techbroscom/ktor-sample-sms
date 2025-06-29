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
    val className: String?,
    val sectionName: String?,
    val academicYearId: String? = null,
    val academicYearName: String? = null // Optional for joined queries
)

@Serializable
data class TeachingClassDto(
    val id: String,
    val className: String?,
    val sectionName: String?,
    val subjectName: String?,
    val subjectId: String?
)