package com.example.models.dto

import com.example.database.tables.StaffClassRole
import kotlinx.serialization.Serializable

@Serializable
data class StaffClassAssignmentDto(
    val id: String? = null,
    val staffId: String,
    val classId: String,
    val academicYearId: String,
    val role: String? = null,
    // Additional fields for joined queries
    val staffName: String? = null,
    val staffEmail: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null
)

@Serializable
data class CreateStaffClassAssignmentRequest(
    val staffId: String,
    val classId: String,
    val academicYearId: String,
    val role: String? = null
)

@Serializable
data class UpdateStaffClassAssignmentRequest(
    val staffId: String,
    val classId: String,
    val academicYearId: String,
    val role: String? = null
)

@Serializable
data class BulkCreateStaffClassAssignmentRequest(
    val staffId: String,
    val classIds: List<String>,
    val academicYearId: String,
    val role: String? = null
)

@Serializable
data class StaffWithClassesDto(
    val staffId: String,
    val staffName: String,
    val staffEmail: String,
    val academicYearId: String,
    val academicYearName: String,
    val classes: List<ClassWithRoleDto>
)

@Serializable
data class ClassWithStaffDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String,
    val staff: List<StaffWithRoleDto>
)

@Serializable
data class ClassWithRoleDto(
    val id: String,
    val className: String,
    val sectionName: String,
    val role: String?
)

@Serializable
data class StaffWithRoleDto(
    val id: String,
    val name: String,
    val email: String,
    val role: String?
)