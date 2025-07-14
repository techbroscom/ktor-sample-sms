package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class FeePaymentDto(
    val id: String? = null,
    val studentFeeId: String,
    val amount: String, // BigDecimal as String for serialization
    val paymentMode: String,
    val paymentDate: String, // LocalDateTime as String
    val receiptNumber: String? = null,
    val remarks: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // Additional fields for joined queries
    val studentId: String? = null,
    val studentName: String? = null,
    val studentEmail: String? = null,
    val feeStructureId: String? = null,
    val feeStructureName: String? = null,
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null,
    val month: String? = null,
    val feeAmount: String? = null,
    val feePaidAmount: String? = null,
    val feeStatus: String? = null
)

@Serializable
data class CreateFeePaymentRequest(
    val studentFeeId: String,
    val amount: String,
    val paymentMode: String,
    val receiptNumber: String? = null,
    val remarks: String? = null
)

@Serializable
data class UpdateFeePaymentRequest(
    val amount: String,
    val paymentMode: String,
    val receiptNumber: String? = null,
    val remarks: String? = null
)

@Serializable
data class PaymentSummaryDto(
    val studentId: String,
    val studentName: String,
    val totalPayments: String,
    val paymentCount: Int,
    val lastPaymentDate: String?,
    val averagePaymentAmount: String,
    val paymentModes: Map<String, Int>
)

@Serializable
data class DailyPaymentReportDto(
    val date: String,
    val totalAmount: String,
    val paymentCount: Int,
    val paymentModes: Map<String, String>, // Mode -> Amount
    val cashAmount: String,
    val onlineAmount: String,
    val bankTransferAmount: String,
    val upiAmount: String,
    val cardAmount: String
)

@Serializable
data class MonthlyPaymentReportDto(
    val month: String,
    val totalAmount: String,
    val paymentCount: Int,
    val paymentModes: Map<String, String>, // Mode -> Amount
    val studentCount: Int,
    val averagePaymentAmount: String
)

@Serializable
data class ClassPaymentSummaryDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val totalPayments: String,
    val paymentCount: Int,
    val studentCount: Int,
    val averagePaymentPerStudent: String
)