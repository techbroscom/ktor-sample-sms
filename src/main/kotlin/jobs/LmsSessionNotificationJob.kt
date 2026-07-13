package com.example.jobs

import com.example.config.TenantDatabaseConfig
import com.example.database.tables.*
import com.example.models.PersonalNotificationRequest
import com.example.repositories.FCMTokenRepository
import com.example.services.FCMService
import com.example.services.TenantService
import com.example.tenant.TenantContextHolder
import io.ktor.server.application.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.time.Duration.Companion.minutes

/**
 * Background job that sends push notifications for LMS sessions:
 * 1. Morning reminder (8 AM): "You have a session today"
 * 2. 15-minute reminder: "Your session starts in 15 minutes"
 *
 * Runs every 5 minutes, checks all tenants for sessions needing
 * notifications.
 */
class LmsSessionNotificationJob(
    private val fcmService: FCMService?,
    private val tenantService: TenantService,
    private val application: Application
) {
    private val logger = LoggerFactory.getLogger(LmsSessionNotificationJob::class.java)
    private var job: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        if (fcmService == null) {
            logger.warn("LMS Notification Job skipped: FCM Service not available")
            return
        }

        job = coroutineScope.launch {
            delay(60_000) // Wait for server startup
            while (isActive) {
                try {
                    processAllTenants()
                } catch (e: Exception) {
                    logger.error("LMS Notification Job error: ${e.message}", e)
                }
                delay(5.minutes)
            }
        }
    }

    private suspend fun processAllTenants() {
        val tenants = tenantService.getAllTenants()

        for (tenant in tenants) {
            try {
                TenantContextHolder.setTenant(tenant)
                processTenantSessions(tenant.schemaName)
            } catch (e: Exception) {
                logger.error("LMS notifications failed for tenant ${tenant.name}: ${e.message}")
            } finally {
                TenantContextHolder.clear()
            }
        }
    }

    private suspend fun processTenantSessions(schemaName: String) {
        val today = LocalDate.now()
        val now = LocalTime.now()

        val tenantDb = TenantDatabaseConfig.getTenantDatabase(schemaName)

        // Get today's sessions with enrolled users
        val sessionsToday = transaction(tenantDb) {
            exec("SET search_path TO $schemaName")

            LmsBatchSessions.selectAll()
                .where {
                    (LmsBatchSessions.scheduledDate eq today) and
                    (LmsBatchSessions.status eq SessionStatus.UPCOMING)
                }
                .map { row ->
                    SessionInfo(
                        id = row[LmsBatchSessions.id],
                        batchId = row[LmsBatchSessions.batchId],
                        sectionId = row[LmsBatchSessions.sectionId],
                        title = row[LmsBatchSessions.title],
                        startTime = row[LmsBatchSessions.startTime],
                        endTime = row[LmsBatchSessions.endTime]
                    )
                }
        }

        if (sessionsToday.isEmpty()) return

        for (session in sessionsToday) {
            val minutesUntilStart = java.time.Duration.between(
                now, session.startTime
            ).toMinutes()

            // Morning notification: between 7:55 AM and 8:05 AM, session is later today
            val isMorningWindow = now.hour == 8 && now.minute < 5
            // 15-minute reminder: 13-17 minutes before start
            val is15MinWindow = minutesUntilStart in 13..17

            if (isMorningWindow || is15MinWindow) {
                val enrolledUserIds = getEnrolledUsers(tenantDb, schemaName, session)
                sendNotifications(session, enrolledUserIds, is15MinWindow)
            }
        }
    }

    private fun getEnrolledUsers(
        tenantDb: org.jetbrains.exposed.sql.Database,
        schemaName: String,
        session: SessionInfo
    ): List<UUID> {
        return transaction(tenantDb) {
            exec("SET search_path TO $schemaName")

            // Users who bought the full batch OR this specific section
            LmsEnrollments.selectAll()
                .where {
                    (LmsEnrollments.batchId eq session.batchId) and
                    (LmsEnrollments.paymentStatus eq PaymentStatus.SUCCESS) and
                    (
                        (LmsEnrollments.purchaseType eq PurchaseType.FULL_BATCH) or
                        (LmsEnrollments.sectionId eq session.sectionId)
                    )
                }
                .map { it[LmsEnrollments.userId] }
                .distinct()
        }
    }

    private suspend fun sendNotifications(
        session: SessionInfo,
        userIds: List<UUID>,
        is15MinReminder: Boolean
    ) {
        if (userIds.isEmpty()) return

        val title: String
        val body: String

        if (is15MinReminder) {
            title = "Session starting soon!"
            body = "\"${session.title}\" starts in 15 minutes. Get ready to join!"
        } else {
            title = "Live session today"
            body = "You have a session today: \"${session.title}\" at ${session.startTime}"
        }

        for (userId in userIds) {
            try {
                fcmService?.sendPersonalNotification(
                    PersonalNotificationRequest(
                        title = title,
                        body = body,
                        userId = userId.toString(),
                        data = mapOf(
                            "type" to "lms_session_reminder",
                            "sessionId" to session.id.toString(),
                            "batchId" to session.batchId.toString(),
                            "is15Min" to is15MinReminder.toString()
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Failed to send LMS notification to user $userId: ${e.message}")
            }
        }

        logger.info("LMS notifications sent: ${userIds.size} users for session \"${session.title}\"")
    }

    fun stop() {
        job?.cancel()
        coroutineScope.cancel()
    }

    private data class SessionInfo(
        val id: UUID,
        val batchId: UUID,
        val sectionId: UUID,
        val title: String,
        val startTime: LocalTime,
        val endTime: LocalTime
    )
}
