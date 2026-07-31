package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class AssessmentRepository {

    private val json = Json { ignoreUnknownKeys = true }

    // ============================================
    // Assessment CRUD
    // ============================================

    suspend fun createAssessment(request: CreateAssessmentRequest, createdBy: UUID): UUID = tenantDbQuery {
        val assessmentId = UUID.randomUUID()
        Assessments.insert {
            it[id] = assessmentId
            it[title] = request.title
            it[description] = request.description
            it[accessType] = AssessmentAccessType.valueOf(request.accessType)
            it[status] = AssessmentStatus.DRAFT
            it[courseId] = request.courseId?.let { UUID.fromString(it) }
            it[sectionId] = request.sectionId?.let { UUID.fromString(it) }
            it[timeLimitMinutes] = request.timeLimitMinutes
            it[passingScore] = request.passingScore
            it[maxAttempts] = request.maxAttempts
            it[shuffleQuestions] = request.shuffleQuestions
            it[showResultImmediately] = request.showResultImmediately
            it[price] = request.price?.let { BigDecimal(it) }
            it[currency] = request.currency
            it[Assessments.createdBy] = createdBy
            it[createdAt] = LocalDateTime.now()
        }
        assessmentId
    }

    suspend fun findAssessmentById(assessmentId: UUID): AssessmentDto? = tenantDbQuery {
        Assessments.selectAll()
            .where { Assessments.id eq assessmentId }
            .singleOrNull()?.let { mapAssessmentRow(it) }
    }

    suspend fun findAllAssessments(
        accessType: String? = null,
        status: String? = null,
        courseId: String? = null
    ): List<AssessmentSummaryDto> = tenantDbQuery {
        var query = Assessments.selectAll()
        accessType?.let { query = query.andWhere { Assessments.accessType eq AssessmentAccessType.valueOf(it) } }
        status?.let { query = query.andWhere { Assessments.status eq AssessmentStatus.valueOf(it) } }
        courseId?.let { query = query.andWhere { Assessments.courseId eq UUID.fromString(it) } }

        query.orderBy(Assessments.createdAt, SortOrder.DESC).map { row ->
            val cId = row[Assessments.courseId]
            val sId = row[Assessments.sectionId]
            val courseName = cId?.let {
                LmsCourses.selectAll().where { LmsCourses.id eq it }
                    .singleOrNull()?.get(LmsCourses.title)
            }
            val sectionName = sId?.let {
                LmsSections.selectAll().where { LmsSections.id eq it }
                    .singleOrNull()?.get(LmsSections.title)
            }
            AssessmentSummaryDto(
                id = row[Assessments.id].toString(),
                title = row[Assessments.title],
                description = row[Assessments.description],
                accessType = row[Assessments.accessType].name,
                status = row[Assessments.status].name,
                courseId = cId?.toString(),
                courseName = courseName,
                sectionId = sId?.toString(),
                sectionName = sectionName,
                totalQuestions = row[Assessments.totalQuestions],
                totalPoints = row[Assessments.totalPoints],
                timeLimitMinutes = row[Assessments.timeLimitMinutes],
                passingScore = row[Assessments.passingScore],
                maxAttempts = row[Assessments.maxAttempts],
                price = row[Assessments.price]?.toString(),
                currency = row[Assessments.currency]
            )
        }
    }

    suspend fun updateAssessment(assessmentId: UUID, request: UpdateAssessmentRequest): Boolean = tenantDbQuery {
        Assessments.update({ Assessments.id eq assessmentId }) {
            request.title?.let { v -> it[title] = v }
            request.description?.let { v -> it[description] = v }
            request.accessType?.let { v -> it[accessType] = AssessmentAccessType.valueOf(v) }
            request.courseId?.let { v -> it[courseId] = UUID.fromString(v) }
            request.sectionId?.let { v -> it[sectionId] = UUID.fromString(v) }
            request.timeLimitMinutes?.let { v -> it[timeLimitMinutes] = v }
            request.passingScore?.let { v -> it[passingScore] = v }
            request.maxAttempts?.let { v -> it[maxAttempts] = v }
            request.shuffleQuestions?.let { v -> it[shuffleQuestions] = v }
            request.showResultImmediately?.let { v -> it[showResultImmediately] = v }
            request.price?.let { v -> it[price] = BigDecimal(v) }
            request.currency?.let { v -> it[currency] = v }
            request.status?.let { v -> it[status] = AssessmentStatus.valueOf(v) }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteAssessment(assessmentId: UUID): Boolean = tenantDbQuery {
        // Delete related data first
        AssessmentAttempts.deleteWhere { AssessmentAttempts.assessmentId eq assessmentId }
        AssessmentAssignments.deleteWhere { AssessmentAssignments.assessmentId eq assessmentId }
        AssessmentPurchases.deleteWhere { AssessmentPurchases.assessmentId eq assessmentId }
        AssessmentBatchEnablements.deleteWhere { AssessmentBatchEnablements.assessmentId eq assessmentId }
        AssessmentQuestions.deleteWhere { AssessmentQuestions.assessmentId eq assessmentId }
        Assessments.deleteWhere { Assessments.id eq assessmentId } > 0
    }

    // ============================================
    // Question CRUD
    // ============================================

    suspend fun createQuestion(assessmentId: UUID, request: CreateQuestionRequest): UUID = tenantDbQuery {
        val questionId = UUID.randomUUID()
        AssessmentQuestions.insert {
            it[id] = questionId
            it[AssessmentQuestions.assessmentId] = assessmentId
            it[questionType] = QuestionType.valueOf(request.questionType)
            it[questionText] = request.questionText
            it[options] = Json.encodeToString(ListSerializer(String.serializer()), request.options)
            it[correctAnswers] = Json.encodeToString(ListSerializer(Int.serializer()), request.correctAnswers)
            it[explanation] = request.explanation
            it[points] = request.points
            it[order] = request.order
            it[createdAt] = LocalDateTime.now()
        }
        // Update assessment question/point counts
        recalculateAssessmentCounts(assessmentId)
        questionId
    }

    suspend fun findQuestionsByAssessmentId(assessmentId: UUID, includeAnswers: Boolean = true): List<AssessmentQuestionDto> = tenantDbQuery {
        AssessmentQuestions.selectAll()
            .where { AssessmentQuestions.assessmentId eq assessmentId }
            .orderBy(AssessmentQuestions.order, SortOrder.ASC)
            .map { row ->
                val optionsList: List<String> = Json.decodeFromString(row[AssessmentQuestions.options])
                val correctList: List<Int> = Json.decodeFromString(row[AssessmentQuestions.correctAnswers])
                AssessmentQuestionDto(
                    id = row[AssessmentQuestions.id].toString(),
                    assessmentId = assessmentId.toString(),
                    questionType = row[AssessmentQuestions.questionType].name,
                    questionText = row[AssessmentQuestions.questionText],
                    options = optionsList,
                    correctAnswers = if (includeAnswers) correctList else null,
                    explanation = if (includeAnswers) row[AssessmentQuestions.explanation] else null,
                    points = row[AssessmentQuestions.points],
                    order = row[AssessmentQuestions.order],
                    createdAt = row[AssessmentQuestions.createdAt].toString(),
                    updatedAt = row[AssessmentQuestions.updatedAt]?.toString()
                )
            }
    }

    suspend fun updateQuestion(questionId: UUID, request: UpdateQuestionRequest): Boolean = tenantDbQuery {
        val question = AssessmentQuestions.selectAll()
            .where { AssessmentQuestions.id eq questionId }
            .singleOrNull() ?: return@tenantDbQuery false

        val assessmentId = question[AssessmentQuestions.assessmentId]

        val updated = AssessmentQuestions.update({ AssessmentQuestions.id eq questionId }) {
            request.questionType?.let { v -> it[questionType] = QuestionType.valueOf(v) }
            request.questionText?.let { v -> it[questionText] = v }
            request.options?.let { v -> it[options] = Json.encodeToString(ListSerializer(String.serializer()), v) }
            request.correctAnswers?.let { v -> it[correctAnswers] = Json.encodeToString(ListSerializer(Int.serializer()), v) }
            request.explanation?.let { v -> it[explanation] = v }
            request.points?.let { v -> it[points] = v }
            request.order?.let { v -> it[order] = v }
            it[updatedAt] = LocalDateTime.now()
        } > 0

        if (updated) recalculateAssessmentCounts(assessmentId)
        updated
    }

    suspend fun deleteQuestion(questionId: UUID): Boolean = tenantDbQuery {
        val question = AssessmentQuestions.selectAll()
            .where { AssessmentQuestions.id eq questionId }
            .singleOrNull() ?: return@tenantDbQuery false

        val assessmentId = question[AssessmentQuestions.assessmentId]
        val deleted = AssessmentQuestions.deleteWhere { AssessmentQuestions.id eq questionId } > 0
        if (deleted) recalculateAssessmentCounts(assessmentId)
        deleted
    }

    private fun recalculateAssessmentCounts(assessmentId: UUID) {
        val questions = AssessmentQuestions.selectAll()
            .where { AssessmentQuestions.assessmentId eq assessmentId }
        val totalQ = questions.count().toInt()
        val totalP = questions.sumOf { it[AssessmentQuestions.points] }
        Assessments.update({ Assessments.id eq assessmentId }) {
            it[totalQuestions] = totalQ
            it[totalPoints] = totalP
            it[updatedAt] = LocalDateTime.now()
        }
    }

    // ============================================
    // Batch Enablement
    // ============================================

    suspend fun toggleBatchEnablement(
        assessmentId: UUID,
        batchId: UUID,
        enabled: Boolean,
        enabledBy: UUID
    ): BatchEnablementDto = tenantDbQuery {
        val existing = AssessmentBatchEnablements.selectAll()
            .where {
                (AssessmentBatchEnablements.assessmentId eq assessmentId) and
                (AssessmentBatchEnablements.batchId eq batchId)
            }.singleOrNull()

        val now = LocalDateTime.now()
        val newStatus = if (enabled) AssessmentEnablementStatus.ENABLED else AssessmentEnablementStatus.DISABLED

        if (existing == null) {
            val id = UUID.randomUUID()
            AssessmentBatchEnablements.insert {
                it[AssessmentBatchEnablements.id] = id
                it[AssessmentBatchEnablements.assessmentId] = assessmentId
                it[AssessmentBatchEnablements.batchId] = batchId
                it[status] = newStatus
                it[enabledAt] = if (enabled) now else null
                it[AssessmentBatchEnablements.enabledBy] = if (enabled) enabledBy else null
                it[createdAt] = now
            }
        } else {
            AssessmentBatchEnablements.update({
                (AssessmentBatchEnablements.assessmentId eq assessmentId) and
                (AssessmentBatchEnablements.batchId eq batchId)
            }) {
                it[status] = newStatus
                it[enabledAt] = if (enabled) now else existing[enabledAt]
                it[AssessmentBatchEnablements.enabledBy] = if (enabled) enabledBy else existing[AssessmentBatchEnablements.enabledBy]
                it[updatedAt] = now
            }
        }

        // Return the enablement
        findBatchEnablement(assessmentId, batchId)!!
    }

    suspend fun findBatchEnablement(assessmentId: UUID, batchId: UUID): BatchEnablementDto? = tenantDbQuery {
        AssessmentBatchEnablements
            .join(LmsBatches, JoinType.INNER, AssessmentBatchEnablements.batchId, LmsBatches.id)
            .selectAll()
            .where {
                (AssessmentBatchEnablements.assessmentId eq assessmentId) and
                (AssessmentBatchEnablements.batchId eq batchId)
            }.singleOrNull()?.let { row ->
                BatchEnablementDto(
                    id = row[AssessmentBatchEnablements.id].toString(),
                    assessmentId = assessmentId.toString(),
                    batchId = batchId.toString(),
                    batchName = row[LmsBatches.name],
                    status = row[AssessmentBatchEnablements.status].name,
                    enabledAt = row[AssessmentBatchEnablements.enabledAt]?.toString(),
                    enabledBy = row[AssessmentBatchEnablements.enabledBy]?.toString(),
                    createdAt = row[AssessmentBatchEnablements.createdAt].toString()
                )
            }
    }

    suspend fun findEnablementsByAssessmentId(assessmentId: UUID): List<BatchEnablementDto> = tenantDbQuery {
        AssessmentBatchEnablements
            .join(LmsBatches, JoinType.INNER, AssessmentBatchEnablements.batchId, LmsBatches.id)
            .selectAll()
            .where { AssessmentBatchEnablements.assessmentId eq assessmentId }
            .map { row ->
                BatchEnablementDto(
                    id = row[AssessmentBatchEnablements.id].toString(),
                    assessmentId = assessmentId.toString(),
                    batchId = row[AssessmentBatchEnablements.batchId].toString(),
                    batchName = row[LmsBatches.name],
                    status = row[AssessmentBatchEnablements.status].name,
                    enabledAt = row[AssessmentBatchEnablements.enabledAt]?.toString(),
                    enabledBy = row[AssessmentBatchEnablements.enabledBy]?.toString(),
                    createdAt = row[AssessmentBatchEnablements.createdAt].toString()
                )
            }
    }

    // ============================================
    // Assignments
    // ============================================

    suspend fun createAssignments(
        assessmentId: UUID,
        userIds: List<UUID>,
        batchId: UUID?,
        assignedBy: UUID,
        dueDate: LocalDateTime?
    ): List<AssessmentAssignmentDto> = tenantDbQuery {
        val created = mutableListOf<UUID>()
        userIds.forEach { userId ->
            // Skip if already assigned
            val exists = AssessmentAssignments.selectAll()
                .where {
                    (AssessmentAssignments.assessmentId eq assessmentId) and
                    (AssessmentAssignments.userId eq userId)
                }.count() > 0
            if (!exists) {
                val assignmentId = UUID.randomUUID()
                AssessmentAssignments.insert {
                    it[id] = assignmentId
                    it[AssessmentAssignments.assessmentId] = assessmentId
                    it[AssessmentAssignments.userId] = userId
                    it[AssessmentAssignments.batchId] = batchId
                    it[AssessmentAssignments.assignedBy] = assignedBy
                    it[AssessmentAssignments.dueDate] = dueDate
                    it[status] = AssessmentAssignmentStatus.ACTIVE
                    it[createdAt] = LocalDateTime.now()
                }
                created.add(assignmentId)
            }
        }
        findAssignmentsByIds(created)
    }

    private fun findAssignmentsByIds(ids: List<UUID>): List<AssessmentAssignmentDto> {
        if (ids.isEmpty()) return emptyList()
        return AssessmentAssignments
            .join(Assessments, JoinType.INNER, AssessmentAssignments.assessmentId, Assessments.id)
            .join(Users, JoinType.INNER, AssessmentAssignments.userId, Users.id)
            .selectAll()
            .where { AssessmentAssignments.id inList ids }
            .map { mapAssignmentRow(it) }
    }

    suspend fun findAssignmentsByAssessmentId(assessmentId: UUID): List<AssessmentAssignmentDto> = tenantDbQuery {
        AssessmentAssignments
            .join(Assessments, JoinType.INNER, AssessmentAssignments.assessmentId, Assessments.id)
            .join(Users, JoinType.INNER, AssessmentAssignments.userId, Users.id)
            .selectAll()
            .where { AssessmentAssignments.assessmentId eq assessmentId }
            .orderBy(AssessmentAssignments.createdAt, SortOrder.DESC)
            .map { mapAssignmentRow(it) }
    }

    suspend fun findAssignmentsByUserId(userId: UUID): List<AssessmentAssignmentDto> = tenantDbQuery {
        AssessmentAssignments
            .join(Assessments, JoinType.INNER, AssessmentAssignments.assessmentId, Assessments.id)
            .join(Users, JoinType.INNER, AssessmentAssignments.userId, Users.id)
            .selectAll()
            .where {
                (AssessmentAssignments.userId eq userId) and
                (AssessmentAssignments.status eq AssessmentAssignmentStatus.ACTIVE)
            }
            .orderBy(AssessmentAssignments.createdAt, SortOrder.DESC)
            .map { mapAssignmentRow(it) }
    }

    // ============================================
    // Purchases
    // ============================================

    suspend fun createPurchase(assessmentId: UUID, userId: UUID, amount: BigDecimal, currency: String): UUID = tenantDbQuery {
        val purchaseId = UUID.randomUUID()
        AssessmentPurchases.insert {
            it[id] = purchaseId
            it[AssessmentPurchases.assessmentId] = assessmentId
            it[AssessmentPurchases.userId] = userId
            it[AssessmentPurchases.amount] = amount
            it[AssessmentPurchases.currency] = currency
            it[paymentStatus] = PaymentStatus.SUCCESS // Mock
            it[paymentProvider] = "MOCK"
            it[paymentReference] = "MOCK_${UUID.randomUUID()}"
            it[purchaseDate] = LocalDateTime.now()
            it[createdAt] = LocalDateTime.now()
        }
        purchaseId
    }

    suspend fun hasPurchase(assessmentId: UUID, userId: UUID): Boolean = tenantDbQuery {
        AssessmentPurchases.selectAll()
            .where {
                (AssessmentPurchases.assessmentId eq assessmentId) and
                (AssessmentPurchases.userId eq userId) and
                (AssessmentPurchases.paymentStatus eq PaymentStatus.SUCCESS)
            }.count() > 0
    }

    // ============================================
    // Attempts
    // ============================================

    suspend fun createAttempt(assessmentId: UUID, userId: UUID): UUID = tenantDbQuery {
        val attemptNumber = AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId)
            }.count().toInt() + 1

        val attemptId = UUID.randomUUID()
        AssessmentAttempts.insert {
            it[id] = attemptId
            it[AssessmentAttempts.assessmentId] = assessmentId
            it[AssessmentAttempts.userId] = userId
            it[AssessmentAttempts.attemptNumber] = attemptNumber
            it[status] = AttemptStatus.IN_PROGRESS
            it[startedAt] = LocalDateTime.now()
            it[createdAt] = LocalDateTime.now()
        }
        attemptId
    }

    suspend fun findAttemptById(attemptId: UUID): AssessmentAttemptDto? = tenantDbQuery {
        AssessmentAttempts
            .join(Assessments, JoinType.INNER, AssessmentAttempts.assessmentId, Assessments.id)
            .join(Users, JoinType.INNER, AssessmentAttempts.userId, Users.id)
            .selectAll()
            .where { AssessmentAttempts.id eq attemptId }
            .singleOrNull()?.let { mapAttemptRow(it) }
    }

    suspend fun submitAttempt(
        attemptId: UUID,
        answers: String,
        score: Int,
        totalPoints: Int,
        percentage: Int,
        passed: Boolean,
        timeTakenSeconds: Int?
    ): Boolean = tenantDbQuery {
        AssessmentAttempts.update({ AssessmentAttempts.id eq attemptId }) {
            it[AssessmentAttempts.answers] = answers
            it[AssessmentAttempts.score] = score
            it[AssessmentAttempts.totalPoints] = totalPoints
            it[AssessmentAttempts.percentage] = percentage
            it[AssessmentAttempts.passed] = passed
            it[AssessmentAttempts.timeTakenSeconds] = timeTakenSeconds
            it[status] = AttemptStatus.SUBMITTED
            it[submittedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun countAttemptsByUser(assessmentId: UUID, userId: UUID): Int = tenantDbQuery {
        AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId)
            }.count().toInt()
    }

    suspend fun hasInProgressAttempt(assessmentId: UUID, userId: UUID): UUID? = tenantDbQuery {
        AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId) and
                (AssessmentAttempts.status eq AttemptStatus.IN_PROGRESS)
            }.singleOrNull()?.get(AssessmentAttempts.id)
    }

    suspend fun findAttemptsByAssessmentId(assessmentId: UUID): List<AssessmentAttemptDto> = tenantDbQuery {
        AssessmentAttempts
            .join(Assessments, JoinType.INNER, AssessmentAttempts.assessmentId, Assessments.id)
            .join(Users, JoinType.INNER, AssessmentAttempts.userId, Users.id)
            .selectAll()
            .where { AssessmentAttempts.assessmentId eq assessmentId }
            .orderBy(AssessmentAttempts.createdAt, SortOrder.DESC)
            .map { mapAttemptRow(it) }
    }

    suspend fun findAttemptsByUserId(userId: UUID): List<AssessmentAttemptDto> = tenantDbQuery {
        AssessmentAttempts
            .join(Assessments, JoinType.INNER, AssessmentAttempts.assessmentId, Assessments.id)
            .join(Users, JoinType.INNER, AssessmentAttempts.userId, Users.id)
            .selectAll()
            .where { AssessmentAttempts.userId eq userId }
            .orderBy(AssessmentAttempts.createdAt, SortOrder.DESC)
            .map { mapAttemptRow(it) }
    }

    suspend fun getBestScore(assessmentId: UUID, userId: UUID): Int? = tenantDbQuery {
        AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId) and
                (AssessmentAttempts.status eq AttemptStatus.SUBMITTED)
            }
            .maxByOrNull { it[AssessmentAttempts.percentage] ?: 0 }
            ?.get(AssessmentAttempts.percentage)
    }

    suspend fun getLastAttemptDate(assessmentId: UUID, userId: UUID): String? = tenantDbQuery {
        AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId)
            }
            .orderBy(AssessmentAttempts.createdAt, SortOrder.DESC)
            .firstOrNull()?.get(AssessmentAttempts.createdAt)?.toString()
    }

    // ============================================
    // Access Check Helpers
    // ============================================

    suspend fun isEnabledForBatch(assessmentId: UUID, batchId: UUID): Boolean = tenantDbQuery {
        AssessmentBatchEnablements.selectAll()
            .where {
                (AssessmentBatchEnablements.assessmentId eq assessmentId) and
                (AssessmentBatchEnablements.batchId eq batchId) and
                (AssessmentBatchEnablements.status eq AssessmentEnablementStatus.ENABLED)
            }.count() > 0
    }

    suspend fun hasAssignment(assessmentId: UUID, userId: UUID): Boolean = tenantDbQuery {
        AssessmentAssignments.selectAll()
            .where {
                (AssessmentAssignments.assessmentId eq assessmentId) and
                (AssessmentAssignments.userId eq userId) and
                (AssessmentAssignments.status eq AssessmentAssignmentStatus.ACTIVE)
            }.count() > 0
    }

    // ============================================
    // Student: Get Available Assessments
    // ============================================

    suspend fun findAvailableAssessmentsForUser(userId: UUID): List<StudentAssessmentDto> = tenantDbQuery {
        val results = mutableListOf<StudentAssessmentDto>()

        // 1. Course-linked assessments where user is enrolled and batch is enabled
        val enrolledBatchIds = LmsEnrollments.selectAll()
            .where {
                (LmsEnrollments.userId eq userId) and
                (LmsEnrollments.paymentStatus eq PaymentStatus.SUCCESS)
            }.map { it[LmsEnrollments.batchId] }

        if (enrolledBatchIds.isNotEmpty()) {
            val enabledAssessmentIds = AssessmentBatchEnablements.selectAll()
                .where {
                    (AssessmentBatchEnablements.batchId inList enrolledBatchIds) and
                    (AssessmentBatchEnablements.status eq AssessmentEnablementStatus.ENABLED)
                }.map { it[AssessmentBatchEnablements.assessmentId] }.distinct()

            if (enabledAssessmentIds.isNotEmpty()) {
                val courseLinked = Assessments.selectAll()
                    .where {
                        (Assessments.id inList enabledAssessmentIds) and
                        (Assessments.status eq AssessmentStatus.READY)
                    }.map { mapToStudentDto(it, userId) }
                results.addAll(courseLinked)
            }
        }

        // 2. Assigned assessments
        val assignedIds = AssessmentAssignments.selectAll()
            .where {
                (AssessmentAssignments.userId eq userId) and
                (AssessmentAssignments.status eq AssessmentAssignmentStatus.ACTIVE)
            }.map { it[AssessmentAssignments.assessmentId] }.distinct()

        if (assignedIds.isNotEmpty()) {
            val assigned = Assessments.selectAll()
                .where {
                    (Assessments.id inList assignedIds) and
                    (Assessments.status eq AssessmentStatus.READY) and
                    (Assessments.id notInList results.map { UUID.fromString(it.id) })
                }.map { mapToStudentDto(it, userId) }
            results.addAll(assigned)
        }

        // 3. Purchased assessments
        val purchasedIds = AssessmentPurchases.selectAll()
            .where {
                (AssessmentPurchases.userId eq userId) and
                (AssessmentPurchases.paymentStatus eq PaymentStatus.SUCCESS)
            }.map { it[AssessmentPurchases.assessmentId] }.distinct()

        if (purchasedIds.isNotEmpty()) {
            val purchased = Assessments.selectAll()
                .where {
                    (Assessments.id inList purchasedIds) and
                    (Assessments.status eq AssessmentStatus.READY) and
                    (Assessments.id notInList results.map { UUID.fromString(it.id) })
                }.map { mapToStudentDto(it, userId) }
            results.addAll(purchased)
        }

        results
    }

    // ============================================
    // Analytics
    // ============================================

    suspend fun getAnalytics(assessmentId: UUID): AssessmentAnalyticsDto? = tenantDbQuery {
        val assessment = Assessments.selectAll()
            .where { Assessments.id eq assessmentId }
            .singleOrNull() ?: return@tenantDbQuery null

        val attempts = AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.status eq AttemptStatus.SUBMITTED)
            }.toList()

        val totalAttempts = attempts.size
        val uniqueStudents = attempts.map { it[AssessmentAttempts.userId] }.distinct().size
        val scores = attempts.mapNotNull { it[AssessmentAttempts.percentage] }
        val passedCount = attempts.count { it[AssessmentAttempts.passed] == true }

        AssessmentAnalyticsDto(
            assessmentId = assessmentId.toString(),
            assessmentTitle = assessment[Assessments.title],
            totalAttempts = totalAttempts,
            uniqueStudents = uniqueStudents,
            averageScore = if (scores.isNotEmpty()) scores.average().toInt() else 0,
            passRate = if (totalAttempts > 0) (passedCount * 100) / totalAttempts else 0,
            highestScore = scores.maxOrNull() ?: 0,
            lowestScore = scores.minOrNull() ?: 0
        )
    }

    // ============================================
    // Mappers
    // ============================================

    private fun mapAssessmentRow(row: ResultRow): AssessmentDto {
        val cId = row[Assessments.courseId]
        val sId = row[Assessments.sectionId]
        val courseName = cId?.let {
            LmsCourses.selectAll().where { LmsCourses.id eq it }
                .singleOrNull()?.get(LmsCourses.title)
        }
        val sectionName = sId?.let {
            LmsSections.selectAll().where { LmsSections.id eq it }
                .singleOrNull()?.get(LmsSections.title)
        }
        return AssessmentDto(
            id = row[Assessments.id].toString(),
            title = row[Assessments.title],
            description = row[Assessments.description],
            accessType = row[Assessments.accessType].name,
            status = row[Assessments.status].name,
            courseId = cId?.toString(),
            courseName = courseName,
            sectionId = sId?.toString(),
            sectionName = sectionName,
            timeLimitMinutes = row[Assessments.timeLimitMinutes],
            passingScore = row[Assessments.passingScore],
            maxAttempts = row[Assessments.maxAttempts],
            shuffleQuestions = row[Assessments.shuffleQuestions],
            showResultImmediately = row[Assessments.showResultImmediately],
            price = row[Assessments.price]?.toString(),
            currency = row[Assessments.currency],
            totalQuestions = row[Assessments.totalQuestions],
            totalPoints = row[Assessments.totalPoints],
            createdBy = row[Assessments.createdBy].toString(),
            createdAt = row[Assessments.createdAt].toString(),
            updatedAt = row[Assessments.updatedAt]?.toString()
        )
    }

    private fun mapAssignmentRow(row: ResultRow): AssessmentAssignmentDto {
        val batchId = row.getOrNull(AssessmentAssignments.batchId)
        val batchName = batchId?.let {
            LmsBatches.selectAll().where { LmsBatches.id eq it }
                .singleOrNull()?.get(LmsBatches.name)
        }
        return AssessmentAssignmentDto(
            id = row[AssessmentAssignments.id].toString(),
            assessmentId = row[AssessmentAssignments.assessmentId].toString(),
            assessmentTitle = row[Assessments.title],
            userId = row[AssessmentAssignments.userId].toString(),
            userName = "${row[Users.firstName]} ${row[Users.lastName]}",
            batchId = batchId?.toString(),
            batchName = batchName,
            assignedBy = row[AssessmentAssignments.assignedBy].toString(),
            status = row[AssessmentAssignments.status].name,
            dueDate = row[AssessmentAssignments.dueDate]?.toString(),
            createdAt = row[AssessmentAssignments.createdAt].toString()
        )
    }

    private fun mapAttemptRow(row: ResultRow): AssessmentAttemptDto {
        return AssessmentAttemptDto(
            id = row[AssessmentAttempts.id].toString(),
            assessmentId = row[AssessmentAttempts.assessmentId].toString(),
            assessmentTitle = row.getOrNull(Assessments.title),
            userId = row[AssessmentAttempts.userId].toString(),
            userName = row.getOrNull(Users.firstName)?.let { "$it ${row.getOrNull(Users.lastName) ?: ""}" },
            attemptNumber = row[AssessmentAttempts.attemptNumber],
            status = row[AssessmentAttempts.status].name,
            score = row[AssessmentAttempts.score],
            totalPoints = row[AssessmentAttempts.totalPoints],
            percentage = row[AssessmentAttempts.percentage],
            passed = row[AssessmentAttempts.passed],
            startedAt = row[AssessmentAttempts.startedAt].toString(),
            submittedAt = row[AssessmentAttempts.submittedAt]?.toString(),
            timeTakenSeconds = row[AssessmentAttempts.timeTakenSeconds]
        )
    }

    private fun mapToStudentDto(row: ResultRow, userId: UUID): StudentAssessmentDto {
        val assessmentId = row[Assessments.id]
        val cId = row[Assessments.courseId]
        val sId = row[Assessments.sectionId]
        val courseName = cId?.let {
            LmsCourses.selectAll().where { LmsCourses.id eq it }
                .singleOrNull()?.get(LmsCourses.title)
        }
        val sectionName = sId?.let {
            LmsSections.selectAll().where { LmsSections.id eq it }
                .singleOrNull()?.get(LmsSections.title)
        }
        val attemptsUsed = AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId)
            }.count().toInt()
        val maxAttempts = row[Assessments.maxAttempts]
        val bestScore = AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId) and
                (AssessmentAttempts.status eq AttemptStatus.SUBMITTED)
            }.maxByOrNull { it[AssessmentAttempts.percentage] ?: 0 }
            ?.get(AssessmentAttempts.percentage)
        val lastAttempt = AssessmentAttempts.selectAll()
            .where {
                (AssessmentAttempts.assessmentId eq assessmentId) and
                (AssessmentAttempts.userId eq userId)
            }.orderBy(AssessmentAttempts.createdAt, SortOrder.DESC)
            .firstOrNull()?.get(AssessmentAttempts.createdAt)?.toString()

        return StudentAssessmentDto(
            id = assessmentId.toString(),
            title = row[Assessments.title],
            description = row[Assessments.description],
            accessType = row[Assessments.accessType].name,
            courseId = cId?.toString(),
            courseName = courseName,
            sectionId = sId?.toString(),
            sectionName = sectionName,
            totalQuestions = row[Assessments.totalQuestions],
            totalPoints = row[Assessments.totalPoints],
            timeLimitMinutes = row[Assessments.timeLimitMinutes],
            passingScore = row[Assessments.passingScore],
            maxAttempts = maxAttempts,
            attemptsUsed = attemptsUsed,
            canAttempt = attemptsUsed < maxAttempts,
            bestScore = bestScore,
            lastAttemptDate = lastAttempt
        )
    }
}
