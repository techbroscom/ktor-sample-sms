package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// ============================================
// Enums
// ============================================

enum class AssessmentStatus {
    DRAFT,
    READY,
    ARCHIVED
}

enum class AssessmentAccessType {
    COURSE_LINKED,   // Auto-available to enrolled students when enabled per batch
    ASSIGNED,        // Admin pushes to specific users/batches
    PURCHASABLE      // Standalone, anyone can buy access
}

enum class QuestionType {
    MCQ,             // Single correct answer
    MULTI_SELECT,    // Multiple correct answers
    TRUE_FALSE       // True or False
}

enum class AssessmentEnablementStatus {
    ENABLED,
    DISABLED
}

enum class AttemptStatus {
    IN_PROGRESS,
    SUBMITTED,
    TIMED_OUT
}

enum class AssessmentAssignmentStatus {
    ACTIVE,
    EXPIRED,
    REVOKED
}

// ============================================
// Assessments — The assessment template
// ============================================

object Assessments : Table("assessments") {
    val id = uuid("id")
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val accessType = enumerationByName("access_type", 20, AssessmentAccessType::class)
    val status = enumerationByName("status", 20, AssessmentStatus::class).default(AssessmentStatus.DRAFT)

    // Course linking (optional — only for COURSE_LINKED type)
    val courseId = uuid("course_id").references(LmsCourses.id).nullable()
    val sectionId = uuid("section_id").references(LmsSections.id).nullable()

    // Assessment settings
    val timeLimitMinutes = integer("time_limit_minutes").nullable() // null = no time limit
    val passingScore = integer("passing_score").default(60) // percentage
    val maxAttempts = integer("max_attempts").default(1) // how many times student can attempt
    val shuffleQuestions = bool("shuffle_questions").default(false)
    val showResultImmediately = bool("show_result_immediately").default(true)

    // Pricing (only for PURCHASABLE type)
    val price = decimal("price", 10, 2).nullable()
    val currency = varchar("currency", 10).default("INR")

    val totalQuestions = integer("total_questions").default(0)
    val totalPoints = integer("total_points").default(0)

    val createdBy = uuid("created_by").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, accessType)
        index(false, status)
        index(false, courseId)
        index(false, sectionId)
    }
}

// ============================================
// Assessment Questions
// ============================================

object AssessmentQuestions : Table("assessment_questions") {
    val id = uuid("id")
    val assessmentId = uuid("assessment_id").references(Assessments.id)
    val questionType = enumerationByName("question_type", 20, QuestionType::class)
    val questionText = text("question_text")
    val options = text("options") // JSON array of option strings
    val correctAnswers = text("correct_answers") // JSON array of correct option indices
    val explanation = text("explanation").nullable() // Shown after attempt
    val points = integer("points").default(1)
    val order = integer("question_order")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, assessmentId)
    }
}

// ============================================
// Assessment Batch Enablement — per-batch toggle for COURSE_LINKED assessments
// ============================================

object AssessmentBatchEnablements : Table("assessment_batch_enablements") {
    val id = uuid("id")
    val assessmentId = uuid("assessment_id").references(Assessments.id)
    val batchId = uuid("batch_id").references(LmsBatches.id)
    val status = enumerationByName("status", 20, AssessmentEnablementStatus::class)
        .default(AssessmentEnablementStatus.DISABLED)
    val enabledAt = datetime("enabled_at").nullable()
    val enabledBy = uuid("enabled_by").references(Users.id).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(assessmentId, batchId)
        index(false, batchId)
        index(false, status)
    }
}

// ============================================
// Assessment Assignments — admin assigns to specific users/batches
// ============================================

object AssessmentAssignments : Table("assessment_assignments") {
    val id = uuid("id")
    val assessmentId = uuid("assessment_id").references(Assessments.id)
    val userId = uuid("user_id").references(Users.id)
    val batchId = uuid("batch_id").references(LmsBatches.id).nullable() // context
    val assignedBy = uuid("assigned_by").references(Users.id)
    val status = enumerationByName("status", 20, AssessmentAssignmentStatus::class)
        .default(AssessmentAssignmentStatus.ACTIVE)
    val dueDate = datetime("due_date").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, assessmentId)
        index(false, userId)
        uniqueIndex(assessmentId, userId) // Prevent duplicate assignments
    }
}

// ============================================
// Assessment Purchases — for PURCHASABLE type
// ============================================

object AssessmentPurchases : Table("assessment_purchases") {
    val id = uuid("id")
    val assessmentId = uuid("assessment_id").references(Assessments.id)
    val userId = uuid("user_id").references(Users.id)
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 10).default("INR")
    val paymentStatus = enumerationByName("payment_status", 20, PaymentStatus::class)
        .default(PaymentStatus.PENDING)
    val paymentReference = varchar("payment_reference", 255).nullable()
    val paymentProvider = varchar("payment_provider", 50).nullable()
    val purchaseDate = datetime("purchase_date").default(LocalDateTime.now())
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, assessmentId)
        index(false, userId)
        uniqueIndex(assessmentId, userId)
    }
}

// ============================================
// Assessment Attempts — student takes the assessment
// ============================================

object AssessmentAttempts : Table("assessment_attempts") {
    val id = uuid("id")
    val assessmentId = uuid("assessment_id").references(Assessments.id)
    val userId = uuid("user_id").references(Users.id)
    val attemptNumber = integer("attempt_number").default(1)
    val status = enumerationByName("status", 20, AttemptStatus::class).default(AttemptStatus.IN_PROGRESS)
    val answers = text("answers").nullable() // JSON: [{questionId, selectedAnswers}]
    val score = integer("score").nullable() // points scored
    val totalPoints = integer("total_points").nullable() // total possible points
    val percentage = integer("percentage").nullable() // score percentage
    val passed = bool("passed").nullable()
    val startedAt = datetime("started_at").default(LocalDateTime.now())
    val submittedAt = datetime("submitted_at").nullable()
    val timeTakenSeconds = integer("time_taken_seconds").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, assessmentId)
        index(false, userId)
        index(false, status)
    }
}
