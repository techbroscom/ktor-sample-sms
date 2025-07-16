package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StudentTransportAssignmentDto(
    val id: String,
    val studentId: String,
    val studentName: String, // Include student name for convenience
    val academicYearId: String,
    val academicYearName: String, // Include academic year name
    val routeId: String,
    val routeName: String, // Include route name
    val stopId: String,
    val stopName: String, // Include stop name
    val startDate: String,
    val endDate: String?,
    val isActive: Boolean
)

@Serializable
data class CreateStudentTransportAssignmentRequest(
    val studentId: String,
    val academicYearId: String,
    val routeId: String,
    val stopId: String,
    val startDate: String, // ISO date format (YYYY-MM-DD)
    val endDate: String? = null, // ISO date format (YYYY-MM-DD)
    val isActive: Boolean = true
)

@Serializable
data class UpdateStudentTransportAssignmentRequest(
    val studentId: String,
    val academicYearId: String,
    val routeId: String,
    val stopId: String,
    val startDate: String, // ISO date format (YYYY-MM-DD)
    val endDate: String? = null, // ISO date format (YYYY-MM-DD)
    val isActive: Boolean
)

@Serializable
data class BulkCreateStudentTransportAssignmentRequest(
    val studentIds: List<String>,
    val academicYearId: String,
    val routeId: String,
    val stopId: String,
    val startDate: String,
    val endDate: String? = null,
    val isActive: Boolean = true
)