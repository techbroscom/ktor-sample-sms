package com.example.services

import com.example.database.tables.*
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.LmsRepository
import io.ktor.http.*
import kotlinx.serialization.json.Json
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class LmsService(
    private val lmsRepository: LmsRepository,
    private val zohoWebinarService: ZohoWebinarService
) {

    // ============================================
    // Course Management
    // ============================================

    suspend fun createCourse(request: CreateCourseRequest, userId: String): CourseDto {
        if (request.title.isBlank()) {
            throw ApiException("Course title is required", HttpStatusCode.BadRequest)
        }
        val createdBy = UUID.fromString(userId)
        val courseId = lmsRepository.createCourse(request, createdBy)
        return lmsRepository.findCourseById(courseId)
            ?: throw ApiException("Failed to create course", HttpStatusCode.InternalServerError)
    }

    suspend fun getCourse(courseId: String): CourseDto {
        val id = UUID.fromString(courseId)
        return lmsRepository.findCourseById(id)
            ?: throw ApiException("Course not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllCourses(status: String?): List<CourseSummaryDto> {
        return lmsRepository.findAllCourses(status)
    }

    suspend fun updateCourse(courseId: String, request: UpdateCourseRequest): CourseDto {
        val id = UUID.fromString(courseId)
        val success = lmsRepository.updateCourse(id, request)
        if (!success) throw ApiException("Course not found", HttpStatusCode.NotFound)
        return lmsRepository.findCourseById(id)!!
    }

    suspend fun deleteCourse(courseId: String) {
        val id = UUID.fromString(courseId)
        val success = lmsRepository.deleteCourse(id)
        if (!success) throw ApiException("Course not found", HttpStatusCode.NotFound)
    }

    // ============================================
    // Section Management
    // ============================================

    suspend fun addSection(courseId: String, request: CreateSectionRequest): CourseDto {
        val cId = UUID.fromString(courseId)
        // Verify course exists
        lmsRepository.findCourseById(cId)
            ?: throw ApiException("Course not found", HttpStatusCode.NotFound)
        lmsRepository.createSection(cId, request)
        return lmsRepository.findCourseById(cId)!!
    }

    suspend fun updateSection(sectionId: String, request: UpdateSectionRequest): Boolean {
        val id = UUID.fromString(sectionId)
        val success = lmsRepository.updateSection(id, request)
        if (!success) throw ApiException("Section not found", HttpStatusCode.NotFound)
        return true
    }

    suspend fun deleteSection(sectionId: String) {
        val id = UUID.fromString(sectionId)
        val success = lmsRepository.deleteSection(id)
        if (!success) throw ApiException("Section not found", HttpStatusCode.NotFound)
    }

    // ============================================
    // Batch Management
    // ============================================

    suspend fun createBatch(courseId: String, request: CreateBatchRequest, userId: String): BatchDto {
        val cId = UUID.fromString(courseId)
        val createdBy = UUID.fromString(userId)

        // Verify course exists
        lmsRepository.findCourseById(cId)
            ?: throw ApiException("Course not found", HttpStatusCode.NotFound)

        if (request.name.isBlank()) {
            throw ApiException("Batch name is required", HttpStatusCode.BadRequest)
        }

        val batchId = lmsRepository.createBatch(cId, request, createdBy)
        return lmsRepository.findBatchById(batchId)
            ?: throw ApiException("Failed to create batch", HttpStatusCode.InternalServerError)
    }

    suspend fun getBatch(batchId: String): BatchDto {
        val id = UUID.fromString(batchId)
        return lmsRepository.findBatchById(id)
            ?: throw ApiException("Batch not found", HttpStatusCode.NotFound)
    }

    suspend fun getBatchesByCourse(courseId: String): List<BatchSummaryDto> {
        val cId = UUID.fromString(courseId)
        return lmsRepository.findBatchesByCourseId(cId)
    }

    suspend fun updateBatch(batchId: String, request: UpdateBatchRequest): BatchDto {
        val id = UUID.fromString(batchId)
        val success = lmsRepository.updateBatch(id, request)
        if (!success) throw ApiException("Batch not found", HttpStatusCode.NotFound)
        return lmsRepository.findBatchById(id)!!
    }

    // ============================================
    // Batch Session Management
    // ============================================

    suspend fun addBatchSession(batchId: String, request: CreateBatchSessionRequest): BatchSessionDto {
        val bId = UUID.fromString(batchId)
        // Verify batch exists
        val batch = lmsRepository.findBatchById(bId)
            ?: throw ApiException("Batch not found", HttpStatusCode.NotFound)

        // Check if Zoho Webinar is configured — auto-create webinar
        val config = lmsRepository.getConfig()
        var meetingLink = request.meetingLink
        var providerMeetingId: String? = null

        if (config != null && config.meetingProvider == MeetingProvider.ZOHO_WEBINAR.name) {
            val credentials = getZohoCredentials(config)
            if (credentials != null) {
                try {
                    val startTime = zohoWebinarService.formatStartTime(
                        date = request.scheduledDate,
                        time = request.startTime
                    )
                    val durationMs = zohoWebinarService.calculateDurationMs(
                        startTime = request.startTime,
                        endTime = request.endTime
                    )

                    val zohoRequest = ZohoCreateWebinarRequest(
                        topic = request.title,
                        agenda = request.description,
                        startTime = startTime,
                        durationMs = durationMs
                    )

                    val result = zohoWebinarService.createWebinar(credentials, zohoRequest)
                    // Store as "meetingKey::instanceId" for later use in registration
                    providerMeetingId = "${result.meetingKey}::${result.instanceId}"
                    meetingLink = result.registrationLink
                } catch (e: Exception) {
                    // Log but don't fail session creation if Zoho call fails
                    println("WARNING: Failed to auto-create Zoho webinar: ${e.message}")
                }
            }
        }

        val sessionId = lmsRepository.createBatchSession(bId, request, meetingLink, providerMeetingId)
        return lmsRepository.findBatchSessionById(sessionId)
            ?: throw ApiException("Failed to create session", HttpStatusCode.InternalServerError)
    }

    suspend fun updateBatchSession(sessionId: String, request: UpdateBatchSessionRequest): BatchSessionDto {
        val id = UUID.fromString(sessionId)
        val success = lmsRepository.updateBatchSession(id, request)
        if (!success) throw ApiException("Session not found", HttpStatusCode.NotFound)
        return lmsRepository.findBatchSessionById(id)!!
    }

    // ============================================
    // Purchase (Mock Payment)
    // ============================================

    suspend fun purchaseBatch(batchId: String, userId: String, request: PurchaseBatchRequest): EnrollmentDto {
        val bId = UUID.fromString(batchId)
        val uId = UUID.fromString(userId)

        val batch = lmsRepository.findBatchById(bId)
            ?: throw ApiException("Batch not found", HttpStatusCode.NotFound)

        // Check seats availability
        batch.maxSeats?.let { max ->
            if (batch.enrolledCount >= max) {
                throw ApiException("Batch is full, no seats available", HttpStatusCode.Conflict)
            }
        }

        // Check if already enrolled
        if (lmsRepository.hasEnrollment(uId, bId, null)) {
            throw ApiException("Already enrolled in this batch", HttpStatusCode.Conflict)
        }

        val amount = BigDecimal(batch.price)
        val enrollmentId = lmsRepository.createEnrollment(
            userId = uId,
            batchId = bId,
            purchaseType = PurchaseType.FULL_BATCH,
            sectionId = null,
            amount = amount,
            currency = batch.currency,
            paymentProvider = "MOCK"
        )

        return EnrollmentDto(
            id = enrollmentId.toString(),
            userId = userId,
            userName = "",
            batchId = batchId,
            batchName = batch.name,
            courseName = batch.courseName,
            purchaseType = PurchaseType.FULL_BATCH.name,
            sectionId = null,
            sectionTitle = null,
            amount = amount.toString(),
            currency = batch.currency,
            paymentStatus = PaymentStatus.SUCCESS.name,
            paymentReference = "MOCK",
            paymentProvider = "MOCK",
            purchaseDate = LocalDateTime.now().toString(),
            createdAt = LocalDateTime.now().toString()
        )
    }

    suspend fun purchaseSection(batchId: String, sectionId: String, userId: String, request: PurchaseSectionRequest): EnrollmentDto {
        val bId = UUID.fromString(batchId)
        val sId = UUID.fromString(sectionId)
        val uId = UUID.fromString(userId)

        val batch = lmsRepository.findBatchById(bId)
            ?: throw ApiException("Batch not found", HttpStatusCode.NotFound)

        // Check if already enrolled for this section or full batch
        if (lmsRepository.hasEnrollment(uId, bId, sId)) {
            throw ApiException("Already enrolled for this section or full batch", HttpStatusCode.Conflict)
        }

        // Get section price
        val sectionPrice = lmsRepository.getBatchSectionPrice(bId, sId)
            ?: throw ApiException("Section pricing not found for this batch", HttpStatusCode.NotFound)

        val enrollmentId = lmsRepository.createEnrollment(
            userId = uId,
            batchId = bId,
            purchaseType = PurchaseType.SECTION,
            sectionId = sId,
            amount = sectionPrice,
            currency = batch.currency,
            paymentProvider = "MOCK"
        )

        return EnrollmentDto(
            id = enrollmentId.toString(),
            userId = userId,
            userName = "",
            batchId = batchId,
            batchName = batch.name,
            courseName = batch.courseName,
            purchaseType = PurchaseType.SECTION.name,
            sectionId = sectionId,
            sectionTitle = null,
            amount = sectionPrice.toString(),
            currency = batch.currency,
            paymentStatus = PaymentStatus.SUCCESS.name,
            paymentReference = "MOCK",
            paymentProvider = "MOCK",
            purchaseDate = LocalDateTime.now().toString(),
            createdAt = LocalDateTime.now().toString()
        )
    }

    // ============================================
    // My Learning
    // ============================================

    suspend fun getMyEnrolledCourses(userId: String): List<MyEnrolledCourseDto> {
        val uId = UUID.fromString(userId)
        return lmsRepository.findEnrollmentsByUserId(uId)
    }

    // ============================================
    // Session Join (Access-gated + Time-gated)
    // ============================================

    suspend fun joinSession(sessionId: String, userId: String): SessionJoinResponse {
        val sId = UUID.fromString(sessionId)
        val uId = UUID.fromString(userId)

        // Get session details
        val session = lmsRepository.findBatchSessionById(sId)
            ?: throw ApiException("Session not found", HttpStatusCode.NotFound)

        val batchId = lmsRepository.getSessionBatchId(sId)!!
        val sectionId = lmsRepository.getSessionSectionId(sId)!!

        // Check enrollment (full batch or specific section)
        if (!lmsRepository.hasEnrollment(uId, batchId, sectionId)) {
            throw ApiException("You are not enrolled for this session", HttpStatusCode.Forbidden)
        }

        // Check time window
        val now = LocalDateTime.now()
        val scheduledDate = LocalDate.parse(session.scheduledDate)
        val startTime = LocalTime.parse(session.startTime)
        val endTime = LocalTime.parse(session.endTime)
        val sessionStart = LocalDateTime.of(scheduledDate, startTime)
        val sessionEnd = LocalDateTime.of(scheduledDate, endTime)

        // Get join window from config (default 10 minutes)
        val config = lmsRepository.getConfig()
        val joinWindowMinutes = config?.sessionJoinWindowMinutes?.toLong() ?: 10L
        val joinWindowStart = sessionStart.minusMinutes(joinWindowMinutes)

        if (now.isBefore(joinWindowStart)) {
            throw ApiException(
                "Session hasn't started yet. You can join ${joinWindowMinutes} minutes before the scheduled time.",
                HttpStatusCode.TooEarly
            )
        }

        if (now.isAfter(sessionEnd)) {
            throw ApiException("Session has already ended", HttpStatusCode.Gone)
        }

        val meetingProvider = config?.meetingProvider ?: "CUSTOM_LINK"

        // If Zoho Webinar — register the student and return personalized joinLink
        if (meetingProvider == MeetingProvider.ZOHO_WEBINAR.name) {
            val credentials = getZohoCredentials(config!!)
            val providerMeetingId = lmsRepository.getSessionProviderMeetingId(sId)

            if (credentials != null && providerMeetingId != null && providerMeetingId.contains("::")) {
                try {
                    val parts = providerMeetingId.split("::")
                    val meetingKey = parts[0]
                    val instanceId = parts[1]

                    // Get user details for registration
                    val userInfo = lmsRepository.getUserBasicInfo(uId)
                    val result = zohoWebinarService.registerAttendee(
                        credentials = credentials,
                        meetingKey = meetingKey,
                        instanceId = instanceId,
                        firstName = userInfo.firstName,
                        lastName = userInfo.lastName,
                        email = userInfo.email
                    )

                    val joinUrl = result.joinUrl.ifBlank {
                        lmsRepository.getSessionMeetingLink(sId) ?: ""
                    }

                    return SessionJoinResponse(
                        sessionId = sessionId,
                        title = session.title,
                        meetingLink = joinUrl,
                        startTime = session.startTime,
                        endTime = session.endTime,
                        provider = MeetingProvider.ZOHO_WEBINAR.name
                    )
                } catch (e: Exception) {
                    println("WARNING: Zoho registration failed, falling back to stored link: ${e.message}")
                }
            }

            // Fallback to stored registration link
            val fallbackLink = lmsRepository.getSessionMeetingLink(sId)
                ?: throw ApiException("Meeting link not available yet", HttpStatusCode.NotFound)

            return SessionJoinResponse(
                sessionId = sessionId,
                title = session.title,
                meetingLink = fallbackLink,
                startTime = session.startTime,
                endTime = session.endTime,
                provider = MeetingProvider.ZOHO_WEBINAR.name
            )
        }

        // Fallback: use stored meeting link (for CUSTOM_LINK or if Zoho failed)
        val meetingLink = lmsRepository.getSessionMeetingLink(sId)
            ?: throw ApiException("Meeting link not available yet", HttpStatusCode.NotFound)

        return SessionJoinResponse(
            sessionId = sessionId,
            title = session.title,
            meetingLink = meetingLink,
            startTime = session.startTime,
            endTime = session.endTime,
            provider = meetingProvider
        )
    }

    // ============================================
    // Zoho Credentials Helper
    // ============================================

    private suspend fun getZohoCredentials(config: LmsConfigDto): ZohoWebinarCredentials? {
        val credentialsJson = lmsRepository.getMeetingCredentialsRaw() ?: return null
        return try {
            Json { ignoreUnknownKeys = true }
                .decodeFromString<ZohoWebinarCredentials>(credentialsJson)
        } catch (e: Exception) {
            println("WARNING: Failed to parse Zoho credentials: ${e.message}")
            null
        }
    }

    // ============================================
    // Zoho Connect (Verify + Save)
    // ============================================

    suspend fun connectZohoWebinar(request: ZohoConnectRequest): ZohoConnectResponse {
        // 1. Verify credentials and fetch user info (ZSOID, ZUID)
        val userInfo = zohoWebinarService.verifyAndFetchUserInfo(
            clientId = request.clientId,
            clientSecret = request.clientSecret,
            refreshToken = request.refreshToken,
            accountsUrl = request.accountsUrl
        )

        // 2. Build full credentials JSON and save config
        val credentialsJson = Json.encodeToString(
            ZohoWebinarCredentials.serializer(),
            ZohoWebinarCredentials(
                clientId = request.clientId,
                clientSecret = request.clientSecret,
                refreshToken = request.refreshToken,
                zsoid = userInfo.zsoid,
                presenterZuid = userInfo.presenterZuid,
                accountsUrl = request.accountsUrl
            )
        )

        // 3. Update LMS config to ZOHO_WEBINAR with full credentials
        val configRequest = UpdateLmsConfigRequest(
            meetingProvider = "ZOHO_WEBINAR",
            meetingCredentials = credentialsJson
        )
        lmsRepository.upsertConfig(configRequest)

        return userInfo
    }

    // ============================================
    // Catalog (Public)
    // ============================================

    suspend fun getCatalog(): List<CatalogCourseDto> {
        return lmsRepository.getCatalog()
    }

    // ============================================
    // Config
    // ============================================

    suspend fun getConfig(): LmsConfigDto? {
        return lmsRepository.getConfig()
    }

    suspend fun updateConfig(request: UpdateLmsConfigRequest): LmsConfigDto {
        return lmsRepository.upsertConfig(request)
    }

    // ============================================
    // Delete All (Testing)
    // ============================================

    suspend fun deleteAllLmsData(): Map<String, Int> {
        return lmsRepository.deleteAllLmsData()
    }
}
