package com.example.models.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Serializable
data class StudentFeeDto(
    val id: String? = null,
    val studentId: String,
    val feeStructureId: String,
    val amount: String, // BigDecimal as String for serialization
    val paidAmount: String,
    val status: String,
    val dueDate: String, // LocalDate as String
    val paidDate: String? = null,
    val month: String,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // Additional fields for joined queries
    val studentName: String? = null,
    val studentEmail: String? = null,
    val feeStructureName: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null,
    val balanceAmount: String? = null // Calculated field
)

@Serializable
data class CreateStudentFeeRequest(
    val studentId: String,
    val feeStructureId: String,
    val amount: String,
    val dueDate: String,
    val month: String
)

@Serializable
data class UpdateStudentFeeRequest(
    val amount: String,
    val dueDate: String,
    val month: String
)

@Serializable
data class BulkCreateStudentFeeRequest(
    val feeStructureId: String,
    val studentIds: List<String>,
    val dueDate: String,
    val month: String
)

@Serializable
data class PayFeeRequest(
    val amount: String,
    val paymentMode: String,
    val receiptNumber: String? = null,
    val remarks: String? = null
)

@Serializable
data class StudentFeesSummaryDto(
    val studentId: String,
    val studentName: String,
    val totalFees: String,
    val totalPaid: String,
    val totalBalance: String,
    val paidCount: Int,
    val partiallyPaidCount: Int,
    val pendingCount: Int,
    val overdueCount: Int
)

@Serializable
data class MonthlyFeeReportDto(
    val month: String,
    val totalFees: String,
    val totalPaid: String,
    val totalBalance: String,
    val studentCount: Int,
    val paidCount: Int,
    val partiallyPaidCount: Int,
    val pendingCount: Int
)

@Serializable
data class ClassFeesSummaryDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val totalFees: String,
    val totalPaid: String,
    val totalBalance: String,
    val studentCount: Int,
    val collectionPercentage: String
)