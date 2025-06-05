package com.example.models.dto

import kotlinx.serialization.Serializable
import java.time.LocalDate

@Serializable
data class ExamDto(
    val id: String? = null,
    val name: String,
    val subjectId: String,
    val classId: String,
    val academicYearId: String,
    val maxMarks: Int,
    val date: String, // LocalDate serialized as String
    // Additional fields for joined queries
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null
)

@Serializable
data class CreateExamRequest(
    val name: String,
    val subjectId: String,
    val classId: String,
    val academicYearId: String,
    val maxMarks: Int,
    val date: String // LocalDate as String in format YYYY-MM-DD
)

@Serializable
data class UpdateExamRequest(
    val name: String,
    val subjectId: String,
    val classId: String,
    val academicYearId: String,
    val maxMarks: Int,
    val date: String // LocalDate as String in format YYYY-MM-DD
)

@Serializable
data class BulkCreateExamRequest(
    val classId: String,
    val academicYearId: String,
    val exams: List<ExamDetails>
)

@Serializable
data class ExamDetails(
    val name: String,
    val subjectId: String,
    val maxMarks: Int,
    val date: String
)

@Serializable
data class ClassExamsDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String,
    val exams: List<ExamDto>
)

@Serializable
data class SubjectExamsDto(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String?,
    val exams: List<ExamDto>
)

@Serializable
data class ExamsByDateDto(
    val date: String,
    val exams: List<ExamDto>
)