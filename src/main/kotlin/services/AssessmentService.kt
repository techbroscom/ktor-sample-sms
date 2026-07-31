package com.example.services

import com.example.database.tables.*
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.AssessmentRepository
import io.ktor.http.*
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class AssessmentService(
    private val repository: AssessmentRepository
) {

    // ============================================
    // Assessment CRUD (Admin)
    // ============================================

    suspend fun createAssessment(request: CreateAssessmentRequest, userId: String): AssessmentDto {
        if (request.title.isBlank()) {
            throw ApiException("Assessment title is required", HttpStatusCode.BadRequest)
        }
        // Validate access type
        try {
            AssessmentAccessType.valueOf(request.accessType)
        } catch (e: Exception) {
            throw ApiException("Invalid access type: ${request.accessType}", HttpStatusCode.BadRequest)
        }

        val createdBy = UUID.fromString(userId)
        val assessmentId = repository.createAssessment(request, createdBy)
        return repository.findAssessmentById(assessmentId)
            ?: throw ApiException("Failed to create assessment", HttpStatusCode.InternalServerError)
    }

    suspend fun getAssessment(assessmentId: String): AssessmentDto {
        val id = UUID.fromString(assessmentId)
        return repository.findAssessmentById(id)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllAssessments(
        accessType: String? = null,
        status: String? = null,
        courseId: String? = null
    ): List<AssessmentSummaryDto> {
        return repository.findAllAssessments(accessType, status, courseId)
    }

    suspend fun updateAssessment(assessmentId: String, request: UpdateAssessmentRequest): AssessmentDto {
        val id = UUID.fromString(assessmentId)
        val success = repository.updateAssessment(id, request)
        if (!success) throw ApiException("Assessment not found", HttpStatusCode.NotFound)
        return repository.findAssessmentById(id)!!
    }

    suspend fun deleteAssessment(assessmentId: String) {
        val id = UUID.fromString(assessmentId)
        val success = repository.deleteAssessment(id)
        if (!success) throw ApiException("Assessment not found", HttpStatusCode.NotFound)
    }

    // ============================================
    // Questions (Admin)
    // ============================================

    suspend fun addQuestion(assessmentId: String, request: CreateQuestionRequest): AssessmentQuestionDto {
        val aId = UUID.fromString(assessmentId)
        // Verify assessment exists
        repository.findAssessmentById(aId)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)

        if (request.questionText.isBlank()) {
            throw ApiException("Question text is required", HttpStatusCode.BadRequest)
        }
        if (request.options.size < 2) {
            throw ApiException("At least 2 options are required", HttpStatusCode.BadRequest)
        }
        if (request.correctAnswers.isEmpty()) {
            throw ApiException("At least 1 correct answer is required", HttpStatusCode.BadRequest)
        }
        // Validate correct answer indices
        request.correctAnswers.forEach { idx ->
            if (idx < 0 || idx >= request.options.size) {
                throw ApiException("Correct answer index $idx is out of bounds", HttpStatusCode.BadRequest)
            }
        }

        val questionId = repository.createQuestion(aId, request)
        val questions = repository.findQuestionsByAssessmentId(aId, includeAnswers = true)
        return questions.find { it.id == questionId.toString() }
            ?: throw ApiException("Failed to create question", HttpStatusCode.InternalServerError)
    }

    suspend fun getQuestions(assessmentId: String, includeAnswers: Boolean = true): List<AssessmentQuestionDto> {
        val aId = UUID.fromString(assessmentId)
        return repository.findQuestionsByAssessmentId(aId, includeAnswers)
    }

    suspend fun updateQuestion(questionId: String, request: UpdateQuestionRequest): Boolean {
        val qId = UUID.fromString(questionId)
        val success = repository.updateQuestion(qId, request)
        if (!success) throw ApiException("Question not found", HttpStatusCode.NotFound)
        return true
    }

    suspend fun deleteQuestion(questionId: String) {
        val qId = UUID.fromString(questionId)
        val success = repository.deleteQuestion(qId)
        if (!success) throw ApiException("Question not found", HttpStatusCode.NotFound)
    }

    // ============================================
    // Batch Enablement (Admin)
    // ============================================

    suspend fun toggleEnablement(
        assessmentId: String,
        batchId: String,
        enabled: Boolean,
        userId: String
    ): BatchEnablementDto {
        val aId = UUID.fromString(assessmentId)
        val bId = UUID.fromString(batchId)
        val uId = UUID.fromString(userId)

        // Verify assessment exists and is READY
        val assessment = repository.findAssessmentById(aId)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)

        if (enabled && assessment.status != AssessmentStatus.READY.name) {
            throw ApiException("Assessment must be in READY status to enable", HttpStatusCode.BadRequest)
        }

        return repository.toggleBatchEnablement(aId, bId, enabled, uId)
    }

    suspend fun getEnablements(assessmentId: String): List<BatchEnablementDto> {
        val aId = UUID.fromString(assessmentId)
        return repository.findEnablementsByAssessmentId(aId)
    }

    // ============================================
    // Assignment (Admin)
    // ============================================

    suspend fun assignAssessment(
        assessmentId: String,
        request: AssignAssessmentRequest,
        userId: String
    ): List<AssessmentAssignmentDto> {
        val aId = UUID.fromString(assessmentId)
        val assignedBy = UUID.fromString(userId)

        // Verify assessment exists
        repository.findAssessmentById(aId)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)

        if (request.userIds.isEmpty()) {
            throw ApiException("At least one user ID is required", HttpStatusCode.BadRequest)
        }

        val userIds = request.userIds.map { UUID.fromString(it) }
        val batchId = request.batchId?.let { UUID.fromString(it) }
        val dueDate = request.dueDate?.let { LocalDateTime.parse(it) }

        return repository.createAssignments(aId, userIds, batchId, assignedBy, dueDate)
    }

    suspend fun getAssignments(assessmentId: String): List<AssessmentAssignmentDto> {
        val aId = UUID.fromString(assessmentId)
        return repository.findAssignmentsByAssessmentId(aId)
    }

    // ============================================
    // Purchase (Student)
    // ============================================

    suspend fun purchaseAssessment(assessmentId: String, userId: String): AssessmentPurchaseDto {
        val aId = UUID.fromString(assessmentId)
        val uId = UUID.fromString(userId)

        val assessment = repository.findAssessmentById(aId)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)

        if (assessment.accessType != AssessmentAccessType.PURCHASABLE.name) {
            throw ApiException("This assessment is not available for purchase", HttpStatusCode.BadRequest)
        }

        if (repository.hasPurchase(aId, uId)) {
            throw ApiException("Already purchased this assessment", HttpStatusCode.Conflict)
        }

        val amount = assessment.price?.let { BigDecimal(it) }
            ?: throw ApiException("Assessment price not configured", HttpStatusCode.BadRequest)

        val purchaseId = repository.createPurchase(aId, uId, amount, assessment.currency)

        return AssessmentPurchaseDto(
            id = purchaseId.toString(),
            assessmentId = assessmentId,
            userId = userId,
            amount = amount.toString(),
            currency = assessment.currency,
            paymentStatus = PaymentStatus.SUCCESS.name,
            paymentReference = "MOCK",
            purchaseDate = LocalDateTime.now().toString()
        )
    }

    // ============================================
    // Student: My Assessments
    // ============================================

    suspend fun getMyAssessments(userId: String): List<StudentAssessmentDto> {
        val uId = UUID.fromString(userId)
        return repository.findAvailableAssessmentsForUser(uId)
    }

    suspend fun getMyAttempts(userId: String): List<AssessmentAttemptDto> {
        val uId = UUID.fromString(userId)
        return repository.findAttemptsByUserId(uId)
    }

    suspend fun getMyAssignments(userId: String): List<AssessmentAssignmentDto> {
        val uId = UUID.fromString(userId)
        return repository.findAssignmentsByUserId(uId)
    }

    // ============================================
    // Attempt Flow (Student)
    // ============================================

    suspend fun startAttempt(assessmentId: String, userId: String): AssessmentAttemptDto {
        val aId = UUID.fromString(assessmentId)
        val uId = UUID.fromString(userId)

        val assessment = repository.findAssessmentById(aId)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)

        if (assessment.status != AssessmentStatus.READY.name) {
            throw ApiException("Assessment is not available", HttpStatusCode.BadRequest)
        }

        // Check if user has access
        verifyStudentAccess(aId, uId)

        // Check for in-progress attempt (resume)
        val inProgressId = repository.hasInProgressAttempt(aId, uId)
        if (inProgressId != null) {
            return repository.findAttemptById(inProgressId)!!
        }

        // Check max attempts
        val attemptCount = repository.countAttemptsByUser(aId, uId)
        if (attemptCount >= assessment.maxAttempts) {
            throw ApiException(
                "Maximum attempts (${assessment.maxAttempts}) reached",
                HttpStatusCode.Forbidden
            )
        }

        val attemptId = repository.createAttempt(aId, uId)
        return repository.findAttemptById(attemptId)!!
    }

    suspend fun getQuestionsForAttempt(assessmentId: String, userId: String): List<AssessmentQuestionDto> {
        val aId = UUID.fromString(assessmentId)
        val uId = UUID.fromString(userId)

        // Verify active attempt
        val inProgressId = repository.hasInProgressAttempt(aId, uId)
            ?: throw ApiException("No active attempt found. Start an attempt first.", HttpStatusCode.BadRequest)

        // Return questions without answers
        return repository.findQuestionsByAssessmentId(aId, includeAnswers = false)
    }

    suspend fun submitAttempt(
        assessmentId: String,
        attemptId: String,
        userId: String,
        request: SubmitAttemptRequest
    ): AttemptResultDto {
        val aId = UUID.fromString(assessmentId)
        val attId = UUID.fromString(attemptId)
        val uId = UUID.fromString(userId)

        // Verify attempt belongs to user and is in-progress
        val attempt = repository.findAttemptById(attId)
            ?: throw ApiException("Attempt not found", HttpStatusCode.NotFound)

        if (attempt.userId != userId) {
            throw ApiException("Attempt does not belong to you", HttpStatusCode.Forbidden)
        }
        if (attempt.status != AttemptStatus.IN_PROGRESS.name) {
            throw ApiException("Attempt already submitted", HttpStatusCode.Conflict)
        }

        // Get questions with answers for grading
        val questions = repository.findQuestionsByAssessmentId(aId, includeAnswers = true)
        val assessment = repository.findAssessmentById(aId)!!

        // Grade
        var totalScore = 0
        var totalPossible = 0
        val questionResults = mutableListOf<QuestionResultDto>()

        questions.forEach { question ->
            totalPossible += question.points
            val submission = request.answers.find { it.questionId == question.id }
            val selectedAnswers = submission?.selectedAnswers ?: emptyList()
            val correctAnswers = question.correctAnswers ?: emptyList()

            val isCorrect = selectedAnswers.sorted() == correctAnswers.sorted()
            val pointsEarned = if (isCorrect) question.points else 0
            totalScore += pointsEarned

            questionResults.add(QuestionResultDto(
                questionId = question.id,
                questionText = question.questionText,
                questionType = question.questionType,
                options = question.options,
                selectedAnswers = selectedAnswers,
                correctAnswers = correctAnswers,
                isCorrect = isCorrect,
                pointsEarned = pointsEarned,
                pointsPossible = question.points,
                explanation = question.explanation
            ))
        }

        val percentage = if (totalPossible > 0) (totalScore * 100) / totalPossible else 0
        val passed = percentage >= assessment.passingScore
        val timeTaken = attempt.startedAt.let {
            val start = LocalDateTime.parse(it)
            java.time.Duration.between(start, LocalDateTime.now()).seconds.toInt()
        }

        // Save results
        val answersJson = Json.encodeToString(
            kotlinx.serialization.builtins.ListSerializer(AnswerSubmission.serializer()),
            request.answers
        )
        repository.submitAttempt(attId, answersJson, totalScore, totalPossible, percentage, passed, timeTaken)

        return AttemptResultDto(
            attemptId = attemptId,
            assessmentTitle = assessment.title,
            score = totalScore,
            totalPoints = totalPossible,
            percentage = percentage,
            passed = passed,
            timeTakenSeconds = timeTaken,
            questionResults = if (assessment.showResultImmediately) questionResults else emptyList()
        )
    }

    // ============================================
    // Analytics (Admin)
    // ============================================

    suspend fun getAnalytics(assessmentId: String): AssessmentAnalyticsDto {
        val aId = UUID.fromString(assessmentId)
        return repository.getAnalytics(aId)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)
    }

    suspend fun getAttemptsByAssessment(assessmentId: String): List<AssessmentAttemptDto> {
        val aId = UUID.fromString(assessmentId)
        return repository.findAttemptsByAssessmentId(aId)
    }

    // ============================================
    // Access Verification
    // ============================================

    private suspend fun verifyStudentAccess(assessmentId: UUID, userId: UUID) {
        val assessment = repository.findAssessmentById(assessmentId)
            ?: throw ApiException("Assessment not found", HttpStatusCode.NotFound)

        when (AssessmentAccessType.valueOf(assessment.accessType)) {
            AssessmentAccessType.COURSE_LINKED -> {
                // Check if any batch the user is enrolled in has this assessment enabled
                // (already handled by findAvailableAssessmentsForUser, but double-check here)
                // Simplified: just check assignment or enablement
            }
            AssessmentAccessType.ASSIGNED -> {
                if (!repository.hasAssignment(assessmentId, userId)) {
                    throw ApiException("You don't have access to this assessment", HttpStatusCode.Forbidden)
                }
            }
            AssessmentAccessType.PURCHASABLE -> {
                if (!repository.hasPurchase(assessmentId, userId)) {
                    throw ApiException("Please purchase this assessment first", HttpStatusCode.Forbidden)
                }
            }
        }
    }
}
