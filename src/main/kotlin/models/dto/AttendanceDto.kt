package com.example.models.dto

import com.example.database.tables.AttendanceStatus
import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class AttendanceDto(
    val id: String? = null,
    val studentId: String,
    val classId: String,
    val date: String, // LocalDate serialized as string (YYYY-MM-DD)
    val status: AttendanceStatus,
    // Additional fields for joined queries
    val studentName: String? = null,
    val studentEmail: String? = null,
    val className: String? = null,
    val sectionName: String? = null
)

@Serializable
data class CreateAttendanceRequest(
    val studentId: String,
    val classId: String,
    val date: String, // YYYY-MM-DD format
    val status: AttendanceStatus
)

@Serializable
data class UpdateAttendanceRequest(
    val studentId: String,
    val classId: String,
    val date: String, // YYYY-MM-DD format
    val status: AttendanceStatus
)

@Serializable
data class BulkCreateAttendanceRequest(
    val classId: String,
    val date: String, // YYYY-MM-DD format
    val attendanceRecords: List<StudentAttendanceRecord>
)

@Serializable
data class StudentAttendanceRecord(
    val studentId: String,
    val status: AttendanceStatus
)

@Serializable
data class ClassAttendanceDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val date: String,
    val totalStudents: Int,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val attendanceRecords: List<AttendanceDto>
)

@Serializable
data class StudentAttendanceDto(
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val attendanceRecords: List<AttendanceDto>
)

@Serializable
data class AttendanceStatsDto(
    val totalDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val lateDays: Int,
    val attendancePercentage: Double
)

@Serializable
data class StudentAttendanceStatsDto(
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val stats: AttendanceStatsDto
)