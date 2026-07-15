package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class LmsRepository {

    // ============================================
    // Course CRUD
    // ============================================

    suspend fun createCourse(request: CreateCourseRequest, createdBy: UUID): UUID = tenantDbQuery {
        val courseId = UUID.randomUUID()
        LmsCourses.insert {
            it[id] = courseId
            it[title] = request.title
            it[description] = request.description
            it[instructor] = request.instructor
            it[thumbnail] = request.thumbnail
            it[category] = request.category
            it[totalDuration] = request.totalDuration
            it[status] = CourseStatus.DRAFT
            it[LmsCourses.createdBy] = createdBy
            it[createdAt] = LocalDateTime.now()
        }
        courseId
    }

    suspend fun findCourseById(courseId: UUID): CourseDto? = tenantDbQuery {
        val course = LmsCourses.selectAll()
            .where { LmsCourses.id eq courseId }
            .singleOrNull() ?: return@tenantDbQuery null

        val sections = findSectionsByCourseId(courseId)
        val batches = findActiveBatchesByCourseId(courseId)

        mapCourseRowToDto(course, sections, batches)
    }

    suspend fun findAllCourses(status: String? = null): List<CourseSummaryDto> = tenantDbQuery {
        var query = LmsCourses.selectAll()
        status?.let {
            query = query.andWhere { LmsCourses.status eq CourseStatus.valueOf(it) }
        }
        query.map { row ->
            val courseId = row[LmsCourses.id]
            val sectionCount = LmsSections.selectAll()
                .where { LmsSections.courseId eq courseId }
                .count().toInt()

            val nextBatch = LmsBatches.selectAll()
                .where { (LmsBatches.courseId eq courseId) and (LmsBatches.status eq BatchStatus.UPCOMING) }
                .orderBy(LmsBatches.startDate, SortOrder.ASC)
                .firstOrNull()

            CourseSummaryDto(
                id = courseId.toString(),
                title = row[LmsCourses.title],
                instructor = row[LmsCourses.instructor],
                thumbnail = row[LmsCourses.thumbnail],
                category = row[LmsCourses.category],
                totalDuration = row[LmsCourses.totalDuration],
                status = row[LmsCourses.status].name,
                sectionCount = sectionCount,
                nextBatchStartDate = nextBatch?.get(LmsBatches.startDate)?.toString(),
                lowestPrice = nextBatch?.get(LmsBatches.price)?.toString()
            )
        }
    }

    suspend fun updateCourse(courseId: UUID, request: UpdateCourseRequest): Boolean = tenantDbQuery {
        LmsCourses.update({ LmsCourses.id eq courseId }) {
            request.title?.let { v -> it[title] = v }
            request.description?.let { v -> it[description] = v }
            request.instructor?.let { v -> it[instructor] = v }
            request.thumbnail?.let { v -> it[thumbnail] = v }
            request.category?.let { v -> it[category] = v }
            request.totalDuration?.let { v -> it[totalDuration] = v }
            request.status?.let { v -> it[status] = CourseStatus.valueOf(v) }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteCourse(courseId: UUID): Boolean = tenantDbQuery {
        LmsCourses.deleteWhere { LmsCourses.id eq courseId } > 0
    }

    // ============================================
    // Section CRUD
    // ============================================

    suspend fun createSection(courseId: UUID, request: CreateSectionRequest): UUID = tenantDbQuery {
        val sectionId = UUID.randomUUID()
        LmsSections.insert {
            it[id] = sectionId
            it[LmsSections.courseId] = courseId
            it[title] = request.title
            it[description] = request.description
            it[order] = request.order
            it[createdAt] = LocalDateTime.now()
        }
        sectionId
    }

    suspend fun updateSection(sectionId: UUID, request: UpdateSectionRequest): Boolean = tenantDbQuery {
        LmsSections.update({ LmsSections.id eq sectionId }) {
            request.title?.let { v -> it[title] = v }
            request.description?.let { v -> it[description] = v }
            request.order?.let { v -> it[order] = v }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteSection(sectionId: UUID): Boolean = tenantDbQuery {
        LmsSections.deleteWhere { LmsSections.id eq sectionId } > 0
    }

    private fun findSectionsByCourseId(courseId: UUID): List<SectionDto> {
        return LmsSections.selectAll()
            .where { LmsSections.courseId eq courseId }
            .orderBy(LmsSections.order, SortOrder.ASC)
            .map { row ->
                val sectionId = row[LmsSections.id]
                val templates = LmsSessionTemplates.selectAll()
                    .where { LmsSessionTemplates.sectionId eq sectionId }
                    .orderBy(LmsSessionTemplates.order, SortOrder.ASC)
                    .map { t ->
                        SessionTemplateDto(
                            id = t[LmsSessionTemplates.id].toString(),
                            sectionId = sectionId.toString(),
                            title = t[LmsSessionTemplates.title],
                            description = t[LmsSessionTemplates.description],
                            order = t[LmsSessionTemplates.order],
                            createdAt = t[LmsSessionTemplates.createdAt].toString(),
                            updatedAt = t[LmsSessionTemplates.updatedAt]?.toString()
                        )
                    }
                SectionDto(
                    id = sectionId.toString(),
                    courseId = courseId.toString(),
                    title = row[LmsSections.title],
                    description = row[LmsSections.description],
                    order = row[LmsSections.order],
                    sessionTemplates = templates,
                    createdAt = row[LmsSections.createdAt].toString(),
                    updatedAt = row[LmsSections.updatedAt]?.toString()
                )
            }
    }

    // ============================================
    // Session Template CRUD
    // ============================================

    suspend fun createSessionTemplate(sectionId: UUID, request: CreateSessionTemplateRequest): UUID = tenantDbQuery {
        val templateId = UUID.randomUUID()
        LmsSessionTemplates.insert {
            it[id] = templateId
            it[LmsSessionTemplates.sectionId] = sectionId
            it[title] = request.title
            it[description] = request.description
            it[order] = request.order
            it[createdAt] = LocalDateTime.now()
        }
        templateId
    }

    suspend fun updateSessionTemplate(templateId: UUID, request: UpdateSessionTemplateRequest): Boolean = tenantDbQuery {
        LmsSessionTemplates.update({ LmsSessionTemplates.id eq templateId }) {
            request.title?.let { v -> it[title] = v }
            request.description?.let { v -> it[description] = v }
            request.order?.let { v -> it[order] = v }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun deleteSessionTemplate(templateId: UUID): Boolean = tenantDbQuery {
        LmsSessionTemplates.deleteWhere { LmsSessionTemplates.id eq templateId } > 0
    }

    // ============================================
    // Batch CRUD
    // ============================================

    suspend fun createBatch(courseId: UUID, request: CreateBatchRequest, createdBy: UUID): UUID = tenantDbQuery {
        val batchId = UUID.randomUUID()
        LmsBatches.insert {
            it[id] = batchId
            it[LmsBatches.courseId] = courseId
            it[name] = request.name
            it[startDate] = LocalDate.parse(request.startDate)
            it[endDate] = LocalDate.parse(request.endDate)
            it[price] = BigDecimal(request.price)
            it[currency] = request.currency
            it[maxSeats] = request.maxSeats
            it[enrolledCount] = 0
            it[status] = BatchStatus.UPCOMING
            it[LmsBatches.createdBy] = createdBy
            it[createdAt] = LocalDateTime.now()
        }

        // Create section pricing entries
        request.sectionPricing.forEach { sp ->
            LmsBatchSections.insert {
                it[id] = UUID.randomUUID()
                it[LmsBatchSections.batchId] = batchId
                it[sectionId] = UUID.fromString(sp.sectionId)
                it[LmsBatchSections.price] = BigDecimal(sp.price)
                it[LmsBatchSections.createdAt] = LocalDateTime.now()
            }
        }

        batchId
    }

    suspend fun findBatchById(batchId: UUID): BatchDto? = tenantDbQuery {
        val batch = LmsBatches.selectAll()
            .where { LmsBatches.id eq batchId }
            .singleOrNull() ?: return@tenantDbQuery null

        val courseId = batch[LmsBatches.courseId]
        val courseName = LmsCourses.selectAll()
            .where { LmsCourses.id eq courseId }
            .singleOrNull()?.get(LmsCourses.title) ?: ""

        val sections = LmsBatchSections
            .join(LmsSections, JoinType.INNER, LmsBatchSections.sectionId, LmsSections.id)
            .selectAll()
            .where { LmsBatchSections.batchId eq batchId }
            .map { row ->
                BatchSectionDto(
                    id = row[LmsBatchSections.id].toString(),
                    sectionId = row[LmsBatchSections.sectionId].toString(),
                    sectionTitle = row[LmsSections.title],
                    price = row[LmsBatchSections.price].toString()
                )
            }

        val sessions = LmsBatchSessions
            .join(LmsSections, JoinType.INNER, LmsBatchSessions.sectionId, LmsSections.id)
            .selectAll()
            .where { LmsBatchSessions.batchId eq batchId }
            .orderBy(LmsBatchSessions.scheduledDate, SortOrder.ASC)
            .map { mapBatchSessionRow(it) }

        val maxSeats = batch[LmsBatches.maxSeats]
        val enrolled = batch[LmsBatches.enrolledCount]

        BatchDto(
            id = batchId.toString(),
            courseId = courseId.toString(),
            courseName = courseName,
            name = batch[LmsBatches.name],
            startDate = batch[LmsBatches.startDate].toString(),
            endDate = batch[LmsBatches.endDate].toString(),
            price = batch[LmsBatches.price].toString(),
            currency = batch[LmsBatches.currency],
            maxSeats = maxSeats,
            enrolledCount = enrolled,
            availableSeats = maxSeats?.let { it - enrolled },
            status = batch[LmsBatches.status].name,
            sections = sections,
            sessions = sessions,
            createdBy = batch[LmsBatches.createdBy].toString(),
            createdAt = batch[LmsBatches.createdAt].toString(),
            updatedAt = batch[LmsBatches.updatedAt]?.toString()
        )
    }

    suspend fun findBatchesByCourseId(courseId: UUID): List<BatchSummaryDto> = tenantDbQuery {
        LmsBatches.selectAll()
            .where { LmsBatches.courseId eq courseId }
            .orderBy(LmsBatches.startDate, SortOrder.DESC)
            .map { mapBatchSummary(it) }
    }

    suspend fun updateBatch(batchId: UUID, request: UpdateBatchRequest): Boolean = tenantDbQuery {
        LmsBatches.update({ LmsBatches.id eq batchId }) {
            request.name?.let { v -> it[name] = v }
            request.startDate?.let { v -> it[startDate] = LocalDate.parse(v) }
            request.endDate?.let { v -> it[endDate] = LocalDate.parse(v) }
            request.price?.let { v -> it[price] = BigDecimal(v) }
            request.currency?.let { v -> it[currency] = v }
            request.maxSeats?.let { v -> it[maxSeats] = v }
            request.status?.let { v -> it[status] = BatchStatus.valueOf(v) }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    private fun findActiveBatchesByCourseId(courseId: UUID): List<BatchSummaryDto> {
        return LmsBatches.selectAll()
            .where {
                (LmsBatches.courseId eq courseId) and
                (LmsBatches.status inList listOf(BatchStatus.UPCOMING, BatchStatus.ONGOING))
            }
            .orderBy(LmsBatches.startDate, SortOrder.ASC)
            .map { mapBatchSummary(it) }
    }

    // ============================================
    // Batch Session CRUD
    // ============================================

    suspend fun createBatchSession(batchId: UUID, request: CreateBatchSessionRequest, meetingLinkOverride: String? = null, providerMeetingId: String? = null): UUID = tenantDbQuery {
        val sessionId = UUID.randomUUID()
        LmsBatchSessions.insert {
            it[id] = sessionId
            it[LmsBatchSessions.batchId] = batchId
            it[sectionId] = UUID.fromString(request.sectionId)
            it[sessionTemplateId] = UUID.fromString(request.sessionTemplateId)
            it[title] = request.title ?: ""
            it[description] = request.description
            it[scheduledDate] = LocalDate.parse(request.scheduledDate)
            it[startTime] = LocalTime.parse(request.startTime)
            it[endTime] = LocalTime.parse(request.endTime)
            it[meetingLink] = meetingLinkOverride ?: request.meetingLink
            it[LmsBatchSessions.providerMeetingId] = providerMeetingId
            it[status] = SessionStatus.UPCOMING
            it[order] = request.order
            it[createdAt] = LocalDateTime.now()
        }
        sessionId
    }

    suspend fun updateBatchSession(sessionId: UUID, request: UpdateBatchSessionRequest): Boolean = tenantDbQuery {
        LmsBatchSessions.update({ LmsBatchSessions.id eq sessionId }) {
            request.title?.let { v -> it[title] = v }
            request.description?.let { v -> it[description] = v }
            request.scheduledDate?.let { v -> it[scheduledDate] = LocalDate.parse(v) }
            request.startTime?.let { v -> it[startTime] = LocalTime.parse(v) }
            request.endTime?.let { v -> it[endTime] = LocalTime.parse(v) }
            request.meetingLink?.let { v -> it[meetingLink] = v }
            request.status?.let { v -> it[status] = SessionStatus.valueOf(v) }
            request.order?.let { v -> it[order] = v }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun findBatchSessionById(sessionId: UUID): BatchSessionDto? = tenantDbQuery {
        LmsBatchSessions
            .join(LmsSections, JoinType.INNER, LmsBatchSessions.sectionId, LmsSections.id)
            .selectAll()
            .where { LmsBatchSessions.id eq sessionId }
            .map { mapBatchSessionRow(it) }
            .singleOrNull()
    }

    suspend fun findSessionsByBatchId(batchId: UUID): List<BatchSessionDto> = tenantDbQuery {
        LmsBatchSessions
            .join(LmsSections, JoinType.INNER, LmsBatchSessions.sectionId, LmsSections.id)
            .selectAll()
            .where { LmsBatchSessions.batchId eq batchId }
            .orderBy(LmsBatchSessions.scheduledDate, SortOrder.ASC)
            .map { mapBatchSessionRow(it) }
    }

    // ============================================
    // Enrollment
    // ============================================

    suspend fun createEnrollment(
        userId: UUID,
        batchId: UUID,
        purchaseType: PurchaseType,
        sectionId: UUID?,
        amount: BigDecimal,
        currency: String,
        paymentProvider: String
    ): UUID = tenantDbQuery {
        val enrollmentId = UUID.randomUUID()
        LmsEnrollments.insert {
            it[id] = enrollmentId
            it[LmsEnrollments.userId] = userId
            it[LmsEnrollments.batchId] = batchId
            it[LmsEnrollments.purchaseType] = purchaseType
            it[LmsEnrollments.sectionId] = sectionId
            it[LmsEnrollments.amount] = amount
            it[LmsEnrollments.currency] = currency
            it[paymentStatus] = PaymentStatus.SUCCESS // Mock payment
            it[LmsEnrollments.paymentProvider] = paymentProvider
            it[paymentReference] = "MOCK_${UUID.randomUUID()}"
            it[purchaseDate] = LocalDateTime.now()
            it[createdAt] = LocalDateTime.now()
        }

        // Increment enrolled count
        LmsBatches.update({ LmsBatches.id eq batchId }) {
            with(SqlExpressionBuilder) {
                it[enrolledCount] = enrolledCount + 1
            }
        }

        enrollmentId
    }

    suspend fun findEnrollmentsByUserId(userId: UUID): List<MyEnrolledCourseDto> = tenantDbQuery {
        val enrollments = LmsEnrollments
            .join(LmsBatches, JoinType.INNER, LmsEnrollments.batchId, LmsBatches.id)
            .join(LmsCourses, JoinType.INNER, LmsBatches.courseId, LmsCourses.id)
            .selectAll()
            .where { (LmsEnrollments.userId eq userId) and (LmsEnrollments.paymentStatus eq PaymentStatus.SUCCESS) }
            .orderBy(LmsEnrollments.purchaseDate, SortOrder.DESC)

        enrollments.map { row ->
            val batchId = row[LmsEnrollments.batchId]
            val sectionId = row[LmsEnrollments.sectionId]

            val sectionTitle = sectionId?.let {
                LmsSections.selectAll().where { LmsSections.id eq it }
                    .singleOrNull()?.get(LmsSections.title)
            }

            // Get upcoming sessions for this batch
            val now = LocalDate.now()
            val upcomingSessions = LmsBatchSessions
                .join(LmsSections, JoinType.INNER, LmsBatchSessions.sectionId, LmsSections.id)
                .selectAll()
                .where {
                    (LmsBatchSessions.batchId eq batchId) and
                    (LmsBatchSessions.scheduledDate greaterEq now) and
                    (LmsBatchSessions.status eq SessionStatus.UPCOMING)
                }
                .orderBy(LmsBatchSessions.scheduledDate, SortOrder.ASC)
                .limit(5)
                .map { mapBatchSessionRow(it) }

            MyEnrolledCourseDto(
                enrollmentId = row[LmsEnrollments.id].toString(),
                courseId = row[LmsCourses.id].toString(),
                courseName = row[LmsCourses.title],
                batchId = batchId.toString(),
                batchName = row[LmsBatches.name],
                purchaseType = row[LmsEnrollments.purchaseType].name,
                sectionId = sectionId?.toString(),
                sectionTitle = sectionTitle,
                batchStartDate = row[LmsBatches.startDate].toString(),
                batchEndDate = row[LmsBatches.endDate].toString(),
                batchStatus = row[LmsBatches.status].name,
                upcomingSessions = upcomingSessions,
                purchaseDate = row[LmsEnrollments.purchaseDate].toString()
            )
        }
    }

    suspend fun hasEnrollment(userId: UUID, batchId: UUID, sectionId: UUID?): Boolean = tenantDbQuery {
        val query = LmsEnrollments.selectAll()
            .where {
                (LmsEnrollments.userId eq userId) and
                (LmsEnrollments.batchId eq batchId) and
                (LmsEnrollments.paymentStatus eq PaymentStatus.SUCCESS)
            }

        if (sectionId != null) {
            // Check if user has full batch OR the specific section
            query.andWhere {
                (LmsEnrollments.purchaseType eq PurchaseType.FULL_BATCH) or
                (LmsEnrollments.sectionId eq sectionId)
            }
        }

        query.count() > 0
    }

    // ============================================
    // LMS Config
    // ============================================

    suspend fun getConfig(): LmsConfigDto? = tenantDbQuery {
        LmsConfig.selectAll().firstOrNull()?.let { row ->
            LmsConfigDto(
                id = row[LmsConfig.id].toString(),
                meetingProvider = row[LmsConfig.meetingProvider].name,
                paymentProvider = row[LmsConfig.paymentProvider].name,
                currency = row[LmsConfig.currency],
                paymentEnabled = row[LmsConfig.paymentEnabled],
                notificationsEnabled = row[LmsConfig.notificationsEnabled],
                sessionJoinWindowMinutes = row[LmsConfig.sessionJoinWindowMinutes],
                createdAt = row[LmsConfig.createdAt].toString(),
                updatedAt = row[LmsConfig.updatedAt]?.toString()
            )
        }
    }

    suspend fun upsertConfig(request: UpdateLmsConfigRequest): LmsConfigDto = tenantDbQuery {
        val existing = LmsConfig.selectAll().firstOrNull()

        if (existing == null) {
            val configId = UUID.randomUUID()
            LmsConfig.insert {
                it[id] = configId
                request.meetingProvider?.let { v -> it[meetingProvider] = MeetingProvider.valueOf(v) }
                request.meetingCredentials?.let { v -> it[meetingCredentials] = v }
                request.paymentProvider?.let { v -> it[paymentProvider] = LmsPaymentProvider.valueOf(v) }
                request.paymentCredentials?.let { v -> it[paymentCredentials] = v }
                request.currency?.let { v -> it[currency] = v }
                request.paymentEnabled?.let { v -> it[paymentEnabled] = v }
                request.notificationsEnabled?.let { v -> it[notificationsEnabled] = v }
                request.sessionJoinWindowMinutes?.let { v -> it[sessionJoinWindowMinutes] = v }
                it[createdAt] = LocalDateTime.now()
            }
        } else {
            val configId = existing[LmsConfig.id]
            LmsConfig.update({ LmsConfig.id eq configId }) {
                request.meetingProvider?.let { v -> it[meetingProvider] = MeetingProvider.valueOf(v) }
                request.meetingCredentials?.let { v -> it[meetingCredentials] = v }
                request.paymentProvider?.let { v -> it[paymentProvider] = LmsPaymentProvider.valueOf(v) }
                request.paymentCredentials?.let { v -> it[paymentCredentials] = v }
                request.currency?.let { v -> it[currency] = v }
                request.paymentEnabled?.let { v -> it[paymentEnabled] = v }
                request.notificationsEnabled?.let { v -> it[notificationsEnabled] = v }
                request.sessionJoinWindowMinutes?.let { v -> it[sessionJoinWindowMinutes] = v }
                it[updatedAt] = LocalDateTime.now()
            }
        }

        getConfigInternal()!!
    }

    private fun getConfigInternal(): LmsConfigDto? {
        return LmsConfig.selectAll().firstOrNull()?.let { row ->
            LmsConfigDto(
                id = row[LmsConfig.id].toString(),
                meetingProvider = row[LmsConfig.meetingProvider].name,
                paymentProvider = row[LmsConfig.paymentProvider].name,
                currency = row[LmsConfig.currency],
                paymentEnabled = row[LmsConfig.paymentEnabled],
                notificationsEnabled = row[LmsConfig.notificationsEnabled],
                sessionJoinWindowMinutes = row[LmsConfig.sessionJoinWindowMinutes],
                createdAt = row[LmsConfig.createdAt].toString(),
                updatedAt = row[LmsConfig.updatedAt]?.toString()
            )
        }
    }

    // ============================================
    // Catalog (Public browsing)
    // ============================================

    suspend fun getCatalog(): List<CatalogCourseDto> = tenantDbQuery {
        val courses = LmsCourses.selectAll()
            .where { LmsCourses.status eq CourseStatus.PUBLISHED }
            .map { row ->
                val courseId = row[LmsCourses.id]
                val sectionCount = LmsSections.selectAll()
                    .where { LmsSections.courseId eq courseId }.count().toInt()

                val batches = LmsBatches.selectAll()
                    .where {
                        (LmsBatches.courseId eq courseId) and
                        (LmsBatches.status inList listOf(BatchStatus.UPCOMING, BatchStatus.ONGOING))
                    }
                    .orderBy(LmsBatches.startDate, SortOrder.ASC)
                    .map { b ->
                        val batchId = b[LmsBatches.id]
                        val sectionPricing = LmsBatchSections
                            .join(LmsSections, JoinType.INNER, LmsBatchSections.sectionId, LmsSections.id)
                            .selectAll()
                            .where { LmsBatchSections.batchId eq batchId }
                            .map { sp ->
                                CatalogSectionPricingDto(
                                    sectionId = sp[LmsBatchSections.sectionId].toString(),
                                    sectionTitle = sp[LmsSections.title],
                                    price = sp[LmsBatchSections.price].toString()
                                )
                            }

                        val maxSeats = b[LmsBatches.maxSeats]
                        val enrolled = b[LmsBatches.enrolledCount]
                        CatalogBatchDto(
                            id = batchId.toString(),
                            name = b[LmsBatches.name],
                            startDate = b[LmsBatches.startDate].toString(),
                            endDate = b[LmsBatches.endDate].toString(),
                            price = b[LmsBatches.price].toString(),
                            currency = b[LmsBatches.currency],
                            maxSeats = maxSeats,
                            enrolledCount = enrolled,
                            availableSeats = maxSeats?.let { it - enrolled },
                            status = b[LmsBatches.status].name,
                            sectionPricing = sectionPricing
                        )
                    }

                CatalogCourseDto(
                    id = courseId.toString(),
                    title = row[LmsCourses.title],
                    description = row[LmsCourses.description],
                    instructor = row[LmsCourses.instructor],
                    thumbnail = row[LmsCourses.thumbnail],
                    category = row[LmsCourses.category],
                    totalDuration = row[LmsCourses.totalDuration],
                    sectionCount = sectionCount,
                    batches = batches
                )
            }
        courses
    }

    // ============================================
    // Helper Mappers
    // ============================================

    private fun mapBatchSessionRow(row: ResultRow): BatchSessionDto {
        val scheduledDate = row[LmsBatchSessions.scheduledDate]
        val startTime = row[LmsBatchSessions.startTime]
        val endTime = row[LmsBatchSessions.endTime]
        val now = LocalDateTime.now()
        val sessionStart = LocalDateTime.of(scheduledDate, startTime)
        val sessionEnd = LocalDateTime.of(scheduledDate, endTime)
        val joinWindowStart = sessionStart.minusMinutes(10)

        val canJoin = now.isAfter(joinWindowStart) && now.isBefore(sessionEnd)

        return BatchSessionDto(
            id = row[LmsBatchSessions.id].toString(),
            batchId = row[LmsBatchSessions.batchId].toString(),
            sectionId = row[LmsBatchSessions.sectionId].toString(),
            sectionTitle = row[LmsSections.title],
            sessionTemplateId = row[LmsBatchSessions.sessionTemplateId].toString(),
            title = row[LmsBatchSessions.title],
            description = row[LmsBatchSessions.description],
            scheduledDate = scheduledDate.toString(),
            startTime = startTime.toString(),
            endTime = endTime.toString(),
            meetingLink = null, // Never exposed directly; use join endpoint
            status = row[LmsBatchSessions.status].name,
            order = row[LmsBatchSessions.order],
            canJoin = canJoin,
            createdAt = row[LmsBatchSessions.createdAt].toString(),
            updatedAt = row[LmsBatchSessions.updatedAt]?.toString()
        )
    }

    private fun mapBatchSummary(row: ResultRow): BatchSummaryDto {
        val maxSeats = row[LmsBatches.maxSeats]
        val enrolled = row[LmsBatches.enrolledCount]
        return BatchSummaryDto(
            id = row[LmsBatches.id].toString(),
            name = row[LmsBatches.name],
            startDate = row[LmsBatches.startDate].toString(),
            endDate = row[LmsBatches.endDate].toString(),
            price = row[LmsBatches.price].toString(),
            currency = row[LmsBatches.currency],
            maxSeats = maxSeats,
            enrolledCount = enrolled,
            availableSeats = maxSeats?.let { it - enrolled },
            status = row[LmsBatches.status].name
        )
    }

    private fun mapCourseRowToDto(
        row: ResultRow,
        sections: List<SectionDto>,
        batches: List<BatchSummaryDto>
    ): CourseDto {
        return CourseDto(
            id = row[LmsCourses.id].toString(),
            title = row[LmsCourses.title],
            description = row[LmsCourses.description],
            instructor = row[LmsCourses.instructor],
            thumbnail = row[LmsCourses.thumbnail],
            category = row[LmsCourses.category],
            totalDuration = row[LmsCourses.totalDuration],
            status = row[LmsCourses.status].name,
            sections = sections,
            activeBatches = batches,
            createdBy = row[LmsCourses.createdBy].toString(),
            createdAt = row[LmsCourses.createdAt].toString(),
            updatedAt = row[LmsCourses.updatedAt]?.toString()
        )
    }

    // Get meeting link for a session (internal - used by service for access check)
    suspend fun getSessionMeetingLink(sessionId: UUID): String? = tenantDbQuery {
        LmsBatchSessions.selectAll()
            .where { LmsBatchSessions.id eq sessionId }
            .singleOrNull()?.get(LmsBatchSessions.meetingLink)
    }

    suspend fun getSessionBatchId(sessionId: UUID): UUID? = tenantDbQuery {
        LmsBatchSessions.selectAll()
            .where { LmsBatchSessions.id eq sessionId }
            .singleOrNull()?.get(LmsBatchSessions.batchId)
    }

    suspend fun getSessionSectionId(sessionId: UUID): UUID? = tenantDbQuery {
        LmsBatchSessions.selectAll()
            .where { LmsBatchSessions.id eq sessionId }
            .singleOrNull()?.get(LmsBatchSessions.sectionId)
    }

    suspend fun getBatchSectionPrice(batchId: UUID, sectionId: UUID): BigDecimal? = tenantDbQuery {
        LmsBatchSections.selectAll()
            .where {
                (LmsBatchSections.batchId eq batchId) and
                (LmsBatchSections.sectionId eq sectionId)
            }
            .singleOrNull()?.get(LmsBatchSections.price)
    }

    // ============================================
    // Zoho Webinar Support
    // ============================================

    suspend fun getSessionProviderMeetingId(sessionId: UUID): String? = tenantDbQuery {
        LmsBatchSessions.selectAll()
            .where { LmsBatchSessions.id eq sessionId }
            .singleOrNull()?.get(LmsBatchSessions.providerMeetingId)
    }

    suspend fun getMeetingCredentialsRaw(): String? = tenantDbQuery {
        LmsConfig.selectAll().firstOrNull()?.get(LmsConfig.meetingCredentials)
    }

    data class UserBasicInfo(
        val firstName: String,
        val lastName: String?,
        val email: String
    )

    suspend fun getUserBasicInfo(userId: UUID): UserBasicInfo = tenantDbQuery {
        val row = Users.selectAll()
            .where { Users.id eq userId }
            .singleOrNull()
            ?: throw RuntimeException("User not found: $userId")

        UserBasicInfo(
            firstName = row[Users.firstName],
            lastName = row[Users.lastName],
            email = row[Users.email]
        )
    }
}
