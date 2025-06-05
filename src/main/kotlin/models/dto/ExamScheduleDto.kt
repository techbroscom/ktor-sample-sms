package com.example.models.dto

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class ExamScheduleDto(
    val id: String? = null,
    val examId: String,
    val classId: String,
    val startTime: String, // ISO datetime string
    val endTime: String,   // ISO datetime string
    // Additional fields for joined queries
    val examName: String? = null,
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val maxMarks: Int? = null,
    val examDate: String? = null, // ISO date string
    val academicYearName: String? = null
)

@Serializable
data class CreateExamScheduleRequest(
    val examId: String,
    val classId: String,
    val startTime: String, // ISO datetime string
    val endTime: String    // ISO datetime string
)

@Serializable
data class UpdateExamScheduleRequest(
    val examId: String,
    val classId: String,
    val startTime: String, // ISO datetime string
    val endTime: String    // ISO datetime string
)

@Serializable
data class BulkCreateExamScheduleRequest(
    val examId: String,
    val classIds: List<String>,
    val startTime: String, // ISO datetime string
    val endTime: String    // ISO datetime string
)

@Serializable
data class ExamWithSchedulesDto(
    val examId: String,
    val examName: String,
    val subjectName: String,
    val subjectCode: String?,
    val maxMarks: Int,
    val examDate: String, // ISO date string
    val academicYearName: String,
    val schedules: List<ScheduleDto>
)

@Serializable
data class ClassWithExamSchedulesDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearName: String,
    val schedules: List<ExamScheduleDetailDto>
)

@Serializable
data class ScheduleDto(
    val id: String,
    val classId: String,
    val className: String,
    val sectionName: String,
    val startTime: String,
    val endTime: String
)

@Serializable
data class ExamScheduleDetailDto(
    val id: String,
    val examId: String,
    val examName: String,
    val subjectName: String,
    val subjectCode: String?,
    val maxMarks: Int,
    val examDate: String,
    val startTime: String,
    val endTime: String
)