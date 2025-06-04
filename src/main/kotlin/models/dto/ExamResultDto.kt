package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class ExamResultDto(
    val id: String? = null,
    val examId: String,
    val studentId: String,
    val marksObtained: Int,
    val grade: String? = null,
    // Additional fields for joined queries
    val examName: String? = null,
    val subjectName: String? = null,
    val subjectCode: String? = null,
    val maxMarks: Int? = null,
    val examDate: String? = null, // ISO date string
    val studentName: String? = null,
    val studentEmail: String? = null,
    val rollNumber: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null
)

@Serializable
data class CreateExamResultRequest(
    val examId: String,
    val studentId: String,
    val marksObtained: Int,
    val grade: String? = null
)

@Serializable
data class UpdateExamResultRequest(
    val examId: String,
    val studentId: String,
    val marksObtained: Int,
    val grade: String? = null
)

@Serializable
data class BulkCreateExamResultRequest(
    val examId: String,
    val results: List<StudentResultRequest>
)

@Serializable
data class StudentResultRequest(
    val studentId: String,
    val marksObtained: Int,
    val grade: String? = null
)

@Serializable
data class ExamWithResultsDto(
    val examId: String,
    val examName: String,
    val subjectName: String,
    val subjectCode: String,
    val maxMarks: Int,
    val examDate: String, // ISO date string
    val academicYearName: String,
    val results: List<StudentResultDto>
)

@Serializable
data class StudentWithExamResultsDto(
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val rollNumber: String,
    val className: String,
    val sectionName: String,
    val academicYearName: String,
    val results: List<ExamResultDetailDto>
)

@Serializable
data class StudentResultDto(
    val id: String,
    val studentId: String,
    val studentName: String,
    val studentEmail: String,
    val rollNumber: String,
    val className: String,
    val sectionName: String,
    val marksObtained: Int,
    val grade: String?
)

@Serializable
data class ExamResultDetailDto(
    val id: String,
    val examId: String,
    val examName: String,
    val subjectName: String,
    val subjectCode: String,
    val maxMarks: Int,
    val examDate: String,
    val marksObtained: Int,
    val grade: String?
)

@Serializable
data class ClassResultSummaryDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val examId: String,
    val examName: String,
    val subjectName: String,
    val totalStudents: Int,
    val studentsAppeared: Int,
    val averageMarks: Double,
    val highestMarks: Int,
    val lowestMarks: Int,
    val passPercentage: Double
)