package com.example.database.tables

import com.example.database.tables.FeesStructures.defaultExpression
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object StudentFees : Table("student_fees") {
    val id = uuid("id").autoGenerate()
    val studentId = uuid("student_id").references(Users.id)
    val feeStructureId = uuid("fee_structure_id").references(FeesStructures.id)
    val amount = decimal("amount", 10, 2)
    val paidAmount = decimal("paid_amount", 10, 2).default(java.math.BigDecimal.ZERO)
    val status = enumeration("status", FeeStatus::class)
    val dueDate = date("due_date")
    val paidDate = date("paid_date").nullable()
    val month = varchar("month", 7) // Format: YYYY-MM
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}

enum class FeeStatus {
    PAID, PARTIALLY_PAID, PENDING
}