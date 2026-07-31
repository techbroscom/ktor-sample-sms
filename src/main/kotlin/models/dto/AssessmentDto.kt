package com.example.models.dto

import kotlinx.serialization.Serializable

// ============================================
// Assessment DTOs
// ============================================

@Serializable
data class AssessmentDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val accessType: String,
    val status: String,
    val courseId: String? = null,
    val courseName: String? = null,
    val sectionId: String? = null,
    val sectionName: String? = null,
    val timeLimitMinutes: Int? = null,
    val passingScore: Int,
    val maxAttempts: Int,
    val shuffleQuestions: Boolean,
    val showResultImmediately: Boolean,
    val price: String? = null,
    val currency: String,
    val totalQuestions: Int,
    val totalPoints: Int,
    val createdBy: String,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class AssessmentSummaryDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val accessType: String,
    val status: String,
    val courseId: String? = null,
    val courseName: String? = null,
    val sectionId: String? = null,
    val sectionName: String? = null,
    val totalQuestions: Int,
    val totalPoints: Int,
    val timeLimitMinutes: Int? = null,
    val passingScore: Int,
    val maxAttempts: Int,
    val price: String? = null,
    val currency: String
)

@Serializable
data class CreateAssessmentRequest(
    val title: String,
    val description: String? = null,
    val accessType: String, // COURSE_LINKED, ASSIGNED, PURCHASABLE
    val courseId: String? = null,
    val sectionId: String? = null,
    val timeLimitMinutes: Int? = null,
    val passingScore: Int = 60,
    val maxAttempts: Int = 1,
    val shuffleQuestions: Boolean = false,
    val showResultImmediately: Boolean = true,
    val price: String? = null,
    val currency: String = "INR"
)

@Serializable
data class UpdateAssessmentRequest(
    val title: String? = null,
    val description: String? = null,
    val accessType: String? = null,
    val courseId: String? = null,
    val sectionId: String? = null,
    val timeLimitMinutes: Int? = null,
    val passingScore: Int? = null,
    val maxAttempts: Int? = null,
    val shuffleQuestions: Boolean? = null,
    val showResultImmediately: Boolean? = null,
    val price: String? = null,
    val currency: String? = null,
    val status: String? = null
)

// ============================================
// Question DTOs
// ============================================

@Serializable
data class AssessmentQuestionDto(
    val id: String,
    val assessmentId: String,
    val questionType: String,
    val questionText: String,
    val options: List<String>,
    val correctAnswers: List<Int>? = null, // null when served to students
    val explanation: String? = null, // null when served to students before submit
    val points: Int,
    val order: Int,
    val createdAt: String,
    val updatedAt: String? = null
)

@Serializable
data class CreateQuestionRequest(
    val questionType: String, // MCQ, MULTI_SELECT, TRUE_FALSE
    val questionText: String,
    val options: List<String>,
    val correctAnswers: List<Int>, // indices of correct options
    val explanation: String? = null,
    val points: Int = 1,
    val order: Int
)

@Serializable
data class UpdateQuestionRequest(
    val questionType: String? = null,
    val questionText: String? = null,
    val options: List<String>? = null,
    val correctAnswers: List<Int>? = null,
    val explanation: String? = null,
    val points: Int? = null,
    val order: Int? = null
)

// ============================================
// Batch Enablement DTOs
// ============================================

@Serializable
data class BatchEnablementDto(
    val id: String,
    val assessmentId: String,
    val batchId: String,
    val batchName: String,
    val status: String,
    val enabledAt: String? = null,
    val enabledBy: String? = null,
    val createdAt: String
)

@Serializable
data class ToggleEnablementRequest(
    val enabled: Boolean
)

// ============================================
// Assignment DTOs
// ============================================

@Serializable
data class AssessmentAssignmentDto(
    val id: String,
    val assessmentId: String,
    val assessmentTitle: String,
    val userId: String,
    val userName: String,
    val batchId: String? = null,
    val batchName: String? = null,
    val assignedBy: String,
    val status: String,
    val dueDate: String? = null,
    val createdAt: String
)

@Serializable
data class AssignAssessmentRequest(
    val userIds: List<String>, // Assign to multiple users at once
    val batchId: String? = null,
    val dueDate: String? = null
)

// ============================================
// Purchase DTOs
// ============================================

@Serializable
data class AssessmentPurchaseDto(
    val id: String,
    val assessmentId: String,
    val userId: String,
    val amount: String,
    val currency: String,
    val paymentStatus: String,
    val paymentReference: String? = null,
    val purchaseDate: String
)

@Serializable
data class PurchaseAssessmentRequest(
    val paymentReference: String? = null
)

// ============================================
// Attempt DTOs
// ============================================

@Serializable
data class AssessmentAttemptDto(
    val id: String,
    val assessmentId: String,
    val assessmentTitle: String? = null,
    val userId: String,
    val userName: String? = null,
    val attemptNumber: Int,
    val status: String,
    val score: Int? = null,
    val totalPoints: Int? = null,
    val percentage: Int? = null,
    val passed: Boolean? = null,
    val startedAt: String,
    val submittedAt: String? = null,
    val timeTakenSeconds: Int? = null
)

@Serializable
data class SubmitAttemptRequest(
    val answers: List<AnswerSubmission>
)

@Serializable
data class AnswerSubmission(
    val questionId: String,
    val selectedAnswers: List<Int> // indices of selected options
)

@Serializable
data class AttemptResultDto(
    val attemptId: String,
    val assessmentTitle: String,
    val score: Int,
    val totalPoints: Int,
    val percentage: Int,
    val passed: Boolean,
    val timeTakenSeconds: Int? = null,
    val questionResults: List<QuestionResultDto> = emptyList()
)

@Serializable
data class QuestionResultDto(
    val questionId: String,
    val questionText: String,
    val questionType: String,
    val options: List<String>,
    val selectedAnswers: List<Int>,
    val correctAnswers: List<Int>,
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val pointsPossible: Int,
    val explanation: String? = null
)

// ============================================
// Student-facing DTOs
// ============================================

@Serializable
data class StudentAssessmentDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val accessType: String,
    val courseId: String? = null,
    val courseName: String? = null,
    val sectionId: String? = null,
    val sectionName: String? = null,
    val totalQuestions: Int,
    val totalPoints: Int,
    val timeLimitMinutes: Int? = null,
    val passingScore: Int,
    val maxAttempts: Int,
    val attemptsUsed: Int,
    val canAttempt: Boolean,
    val bestScore: Int? = null,
    val lastAttemptDate: String? = null
)

// ============================================
// Admin Analytics DTOs
// ============================================

@Serializable
data class AssessmentAnalyticsDto(
    val assessmentId: String,
    val assessmentTitle: String,
    val totalAttempts: Int,
    val uniqueStudents: Int,
    val averageScore: Int,
    val passRate: Int, // percentage
    val highestScore: Int,
    val lowestScore: Int
)
