package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class VisitorStatus {
    SCHEDULED,      // Pre-registered, not yet arrived
    CHECKED_IN,     // Currently on premises
    CHECKED_OUT,    // Visit completed
    CANCELLED,      // Scheduled visit cancelled
    NO_SHOW         // Scheduled but didn't arrive
}

object Visitors : Table("visitors") {
    val id = uuid("id")
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).nullable()
    val mobileNumber = varchar("mobile_number", 15)
    val organizationName = varchar("organization_name", 255).nullable()
    val purposeOfVisit = varchar("purpose_of_visit", 500)
    val visitDate = date("visit_date")
    val expectedCheckInTime = datetime("expected_check_in_time")
    val actualCheckInTime = datetime("actual_check_in_time").nullable()
    val checkOutTime = datetime("check_out_time").nullable()
    val status = enumerationByName("status", 20, VisitorStatus::class)
    val hostUserId = uuid("host_user_id").references(Users.id)
    val identificationProof = varchar("identification_proof", 50).nullable()
    val identificationNumber = varchar("identification_number", 100).nullable()
    val photoUrl = varchar("photo_url", 500).nullable()
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()
    val createdBy = uuid("created_by").references(Users.id)

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, hostUserId)
        index(false, visitDate)
        index(false, status)
        index(false, visitDate, status)
    }
}
