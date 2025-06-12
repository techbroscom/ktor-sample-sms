package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StudentAssignmentDto(
    val id: String? = null,
    val studentId: String,
    val classId: String,
    val academicYearId: String,
    // Additional fields for joined queries
    val studentName: String? = null,
    val studentEmail: String? = null,
    val studentPhone: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null
)

@Serializable
data class CreateStudentAssignmentRequest(
    val studentId: String,
    val classId: String,
    val academicYearId: String
)

@Serializable
data class UpdateStudentAssignmentRequest(
    val studentId: String,
    val classId: String,
    val academicYearId: String
)

@Serializable
data class BulkCreateStudentAssignmentRequest(
    val studentIds: List<String>,
    val classId: String,
    val academicYearId: String
)

@Serializable
data class BulkTransferStudentsRequest(
    val studentIds: List<String>,
    val fromClassId: String,
    val toClassId: String,
    val academicYearId: String
)

@Serializable
data class ClassWithStudentsDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String,
    val students: List<StudentDto>
)

@Serializable
data class StudentWithClassDto(
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val studentPhone: String?,
    val classAssignments: List<ClassAssignmentDto>
)

@Serializable
data class ClassAssignmentDto(
    val assignmentId: String,
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String
)

@Serializable
data class StudentDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val mobileNumber: String?
)

@Serializable
data class StudentAssignmentSummaryDto(
    val totalAssignments: Int,
    val assignmentsByAcademicYear: Map<String, Int>,
    val assignmentsByClass: Map<String, Int>,
    val mostRecentAssignment: StudentAssignmentDto?
)

@Serializable
data class ClassCapacityDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val currentStudentCount: Int,
    val maxCapacity: Int?,
    val availableSpots: Int?
)

@Serializable
data class AcademicYearSummaryDto(
    val academicYearId: String,
    val academicYearName: String,
    val totalStudents: Int,
    val totalClasses: Int,
    val averageStudentsPerClass: Double,
    val classesWithStudents: List<ClassCapacityDto>
)