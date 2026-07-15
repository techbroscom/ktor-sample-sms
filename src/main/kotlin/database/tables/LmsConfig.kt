package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class MeetingProvider {
    CUSTOM_LINK,    // Admin manually pastes Zoom/Meet link
    ZOOM,           // Auto-generate via Zoom API (future)
    GOOGLE_MEET,    // Auto-generate via Google Calendar API (future)
    TEAMS,          // Microsoft Teams (future)
    ZOHO_WEBINAR    // Auto-generate via Zoho Webinar API
}

enum class LmsPaymentProvider {
    MOCK,       // Instant success (development/testing)
    RAZORPAY,   // Razorpay integration
    STRIPE,     // Stripe integration
    PAYU        // PayU integration
}

/**
 * Per-tenant LMS configuration.
 * Each institution configures their own meeting provider and payment gateway.
 */
object LmsConfig : Table("lms_config") {
    val id = uuid("id")
    val meetingProvider = enumerationByName("meeting_provider", 20, MeetingProvider::class)
        .default(MeetingProvider.CUSTOM_LINK)
    val meetingCredentials = text("meeting_credentials").nullable() // Encrypted JSON
    val paymentProvider = enumerationByName("payment_provider", 20, LmsPaymentProvider::class)
        .default(LmsPaymentProvider.MOCK)
    val paymentCredentials = text("payment_credentials").nullable() // Encrypted JSON
    val currency = varchar("currency", 10).default("INR")
    val paymentEnabled = bool("payment_enabled").default(false)
    val notificationsEnabled = bool("notifications_enabled").default(true)
    val sessionJoinWindowMinutes = integer("session_join_window_minutes").default(10) // How early user can join
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
