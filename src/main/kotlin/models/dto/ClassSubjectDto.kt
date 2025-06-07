package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClassSubjectDto(
    val id: String? = null,
    val classId: String,
    val subjectId: String,
    val academicYearId: String,
    // Additional fields for joined queries
    val className: String? = null,
    val sectionName: String? = null,
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val academicYearName: String? = null
)

@Serializable
data class CreateClassSubjectRequest(
    val classId: String,
    val subjectId: String,
    val academicYearId: String? = null
)

@Serializable
data class UpdateClassSubjectRequest(
    val classId: String,
    val subjectId: String,
    val academicYearId: String? = null
)

@Serializable
data class BulkCreateClassSubjectRequest(
    val classId: String,
    val subjectIds: List<String>,
    val academicYearId: String? = null
)

@Serializable
data class ClassWithSubjectsDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String? = null,
    val academicYearName: String,
    val subjects: List<SubjectDto>
)

@Serializable
data class SubjectWithClassesDto(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String?,
    val classes: List<ClassDto>
)