package com.example.database.tables

import com.example.database.tables.StudentFees.defaultExpression
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object FeePayments : Table("fee_payments") {
    val id = uuid("id").autoGenerate()
    val studentFeeId = uuid("student_fee_id").references(StudentFees.id)
    val amount = decimal("amount", 10, 2)
    val paymentMode = enumeration("payment_mode", PaymentMode::class)
    val paymentDate = datetime("payment_date")
    val receiptNumber = varchar("receipt_number", 100).nullable()
    val remarks = text("remarks").nullable()
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)

    override val primaryKey = PrimaryKey(id)

}

enum class PaymentMode {
    CASH, ONLINE, BANK_TRANSFER, UPI, CARD
}