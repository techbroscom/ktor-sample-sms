package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StaffSubjectAssignmentDto(
    val id: String? = null,
    val staffId: String,
    val classSubjectId: String,
    val classId: String,
    val academicYearId: String,
    // Additional fields for joined queries
    val staffName: String? = null,
    val staffEmail: String? = null,
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null
)

@Serializable
data class CreateStaffSubjectAssignmentRequest(
    val staffId: String,
    val classSubjectId: String,
    val classId: String,
    val academicYearId: String
)

@Serializable
data class UpdateStaffSubjectAssignmentRequest(
    val staffId: String,
    val classSubjectId: String,
    val classId: String,
    val academicYearId: String
)

@Serializable
data class BulkCreateStaffSubjectAssignmentRequest(
    val staffId: String,
    val classSubjectIds: List<String>,
    val classId: String,
    val academicYearId: String
)

@Serializable
data class StaffWithSubjectsDto(
    val staffId: String,
    val staffName: String,
    val staffEmail: String,
    val academicYearId: String,
    val academicYearName: String,
    val subjects: List<SubjectWithClassDto>
)

@Serializable
data class ClassWithSubjectStaffDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String,
    val subjects: List<SubjectWithStaffDto>
)

@Serializable
data class SubjectWithClassDto(
    val id: String,
    val classSubjectId: String,
    val subjectName: String,
    val subjectCode: String?,
    val className: String,
    val sectionName: String
)

@Serializable
data class SubjectWithStaffDto(
    val id: String,
    val classSubjectId: String,
    val subjectName: String,
    val subjectCode: String?,
    val staff: List<StaffBasicDto>
)

@Serializable
data class StaffBasicDto(
    val id: String,
    val name: String,
    val email: String
)