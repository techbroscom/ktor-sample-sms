package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

enum class PurchaseType {
    FULL_BATCH,
    SECTION
}

enum class PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED
}

object LmsEnrollments : Table("lms_enrollments") {
    val id = uuid("id")
    val userId = uuid("user_id").references(Users.id)
    val batchId = uuid("batch_id").references(LmsBatches.id)
    val purchaseType = enumerationByName("purchase_type", 20, PurchaseType::class)
    val sectionId = uuid("section_id").references(LmsSections.id).nullable() // Only for SECTION purchase
    val amount = decimal("amount", 10, 2)
    val currency = varchar("currency", 10).default("INR")
    val paymentStatus = enumerationByName("payment_status", 20, PaymentStatus::class).default(PaymentStatus.PENDING)
    val paymentReference = varchar("payment_reference", 255).nullable() // PG transaction ID
    val paymentProvider = varchar("payment_provider", 50).nullable() // MOCK / RAZORPAY / STRIPE
    val purchaseDate = datetime("purchase_date").default(LocalDateTime.now())
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, userId)
        index(false, batchId)
        index(false, paymentStatus)
        uniqueIndex(userId, batchId, sectionId) // Prevent duplicate enrollments
    }
}
