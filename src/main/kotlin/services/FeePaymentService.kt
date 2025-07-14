package com.example.services

import com.example.database.tables.PaymentMode
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.FeePaymentRepository
import com.example.repositories.StudentFeeRepository
import io.ktor.http.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
import java.util.*

class FeePaymentService(
    private val feePaymentRepository: FeePaymentRepository,
    private val studentFeeRepository: StudentFeeRepository
) {

    suspend fun createFeePayment(request: CreateFeePaymentRequest): FeePaymentDto {
        validateCreateRequest(request)

        // Validate that student fee exists
        val studentFee = studentFeeRepository.findById(request.studentFeeId)
            ?: throw ApiException("Student fee record not found", HttpStatusCode.NotFound)

        // Validate payment amount
        val currentPaidAmount = BigDecimal(studentFee.paidAmount)
        val paymentAmount = BigDecimal(request.amount)
        val totalAmount = BigDecimal(studentFee.amount)
        val newPaidAmount = currentPaidAmount + paymentAmount

        if (newPaidAmount > totalAmount) {
            throw ApiException("Payment amount exceeds the remaining balance", HttpStatusCode.BadRequest)
        }

        // Check receipt number uniqueness if provided
        request.receiptNumber?.let { receiptNumber ->
            if (receiptNumber.isNotBlank() && feePaymentRepository.checkReceiptNumberExists(receiptNumber)) {
                throw ApiException("Receipt number already exists", HttpStatusCode.Conflict)
            }
        }

        val paymentId = feePaymentRepository.create(request)

        // Update student fee record
        studentFeeRepository.updatePayment(request.studentFeeId, newPaidAmount)

        return getFeePaymentById(paymentId)
    }

    suspend fun getFeePaymentById(id: String): FeePaymentDto {
        validateUUID(id, "Payment ID")
        return feePaymentRepository.findById(id)
            ?: throw ApiException("Payment record not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllFeePayments(): List<FeePaymentDto> {
        return feePaymentRepository.findAll()
    }

    suspend fun updateFeePayment(id: String, request: UpdateFeePaymentRequest): FeePaymentDto {
        validateUUID(id, "Payment ID")
        validateUpdateRequest(request)

        val existingPayment = getFeePaymentById(id)

        // Check receipt number uniqueness if provided and changed
        request.receiptNumber?.let { receiptNumber ->
            if (receiptNumber.isNotBlank() &&
                receiptNumber != existingPayment.receiptNumber &&
                feePaymentRepository.checkReceiptNumberExists(receiptNumber, id)) {
                throw ApiException("Receipt number already exists", HttpStatusCode.Conflict)
            }
        }

        // If amount is changed, validate against student fee
        if (request.amount != existingPayment.amount) {
            val studentFee = studentFeeRepository.findById(existingPayment.studentFeeId)
                ?: throw ApiException("Student fee record not found", HttpStatusCode.NotFound)

            val currentPaidAmount = BigDecimal(studentFee.paidAmount)
            val oldPaymentAmount = BigDecimal(existingPayment.amount)
            val newPaymentAmount = BigDecimal(request.amount)
            val totalAmount = BigDecimal(studentFee.amount)

            val adjustedPaidAmount = currentPaidAmount - oldPaymentAmount + newPaymentAmount

            if (adjustedPaidAmount > totalAmount) {
                throw ApiException("Updated payment amount exceeds the remaining balance", HttpStatusCode.BadRequest)
            }

            // Update student fee record
            studentFeeRepository.updatePayment(existingPayment.studentFeeId, adjustedPaidAmount)
        }

        val updated = feePaymentRepository.update(id, request)
        if (!updated) {
            throw ApiException("Payment record not found", HttpStatusCode.NotFound)
        }

        return getFeePaymentById(id)
    }

    suspend fun deleteFeePayment(id: String) {
        validateUUID(id, "Payment ID")

        val payment = getFeePaymentById(id)

        // Get student fee and update paid amount
        val studentFee = studentFeeRepository.findById(payment.studentFeeId)
            ?: throw ApiException("Student fee record not found", HttpStatusCode.NotFound)

        val currentPaidAmount = BigDecimal(studentFee.paidAmount)
        val paymentAmount = BigDecimal(payment.amount)
        val newPaidAmount = currentPaidAmount - paymentAmount

        // Update student fee record
        studentFeeRepository.updatePayment(payment.studentFeeId, newPaidAmount)

        val deleted = feePaymentRepository.delete(id)
        if (!deleted) {
            throw ApiException("Payment record not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getFeePaymentsByStudentFeeId(studentFeeId: String): List<FeePaymentDto> {
        validateUUID(studentFeeId, "Student Fee ID")
        studentFeeRepository.findById(studentFeeId)
            ?: throw ApiException("Student fee record not found", HttpStatusCode.NotFound)
        return feePaymentRepository.findByStudentFeeId(studentFeeId)
    }

    suspend fun getFeePaymentsByStudentId(studentId: String): List<FeePaymentDto> {
        validateUUID(studentId, "Student ID")
        return feePaymentRepository.findByStudentId(studentId)
    }

    suspend fun getFeePaymentsByPaymentMode(paymentMode: String): List<FeePaymentDto> {
        val mode = try {
            PaymentMode.valueOf(paymentMode.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid payment mode: $paymentMode", HttpStatusCode.BadRequest)
        }
        return feePaymentRepository.findByPaymentMode(mode)
    }

    suspend fun getFeePaymentsByDateRange(startDate: String, endDate: String): List<FeePaymentDto> {
        val start = parseDateTime(startDate, "Start date")
        val end = parseDateTime(endDate, "End date")

        if (start.isAfter(end)) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        return feePaymentRepository.findByDateRange(start, end)
    }

    suspend fun getFeePaymentsByMonth(month: String): List<FeePaymentDto> {
        if (month.isBlank()) {
            throw ApiException("Month cannot be empty", HttpStatusCode.BadRequest)
        }
        return feePaymentRepository.findByMonth(month)
    }

    suspend fun getFeePaymentsByClassAndDateRange(
        classId: String,
        startDate: String,
        endDate: String
    ): List<FeePaymentDto> {
        validateUUID(classId, "Class ID")
        val start = parseDateTime(startDate, "Start date")
        val end = parseDateTime(endDate, "End date")

        if (start.isAfter(end)) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        return feePaymentRepository.findByClassAndDateRange(classId, start, end)
    }

    suspend fun getFeePaymentByReceiptNumber(receiptNumber: String): FeePaymentDto? {
        if (receiptNumber.isBlank()) {
            throw ApiException("Receipt number cannot be empty", HttpStatusCode.BadRequest)
        }
        return feePaymentRepository.findByReceiptNumber(receiptNumber)
    }

    suspend fun getPaymentSummary(studentId: String): PaymentSummaryDto? {
        validateUUID(studentId, "Student ID")
        return feePaymentRepository.getPaymentSummary(studentId)
    }

    suspend fun getDailyPaymentReport(date: String): DailyPaymentReportDto {
        val reportDate = parseDate(date, "Date")
        return feePaymentRepository.getDailyPaymentReport(reportDate)
    }

    suspend fun getMonthlyPaymentReport(month: String): MonthlyPaymentReportDto {
        if (month.isBlank()) {
            throw ApiException("Month cannot be empty", HttpStatusCode.BadRequest)
        }
        return feePaymentRepository.getMonthlyPaymentReport(month)
    }

    suspend fun getClassPaymentSummary(
        classId: String,
        startDate: String,
        endDate: String
    ): ClassPaymentSummaryDto? {
        validateUUID(classId, "Class ID")
        val start = parseDateTime(startDate, "Start date")
        val end = parseDateTime(endDate, "End date")

        if (start.isAfter(end)) {
            throw ApiException("Start date must be before end date", HttpStatusCode.BadRequest)
        }

        return feePaymentRepository.getClassPaymentSummary(classId, start, end)
    }

    suspend fun deletePaymentsByStudentFeeId(studentFeeId: String): Int {
        validateUUID(studentFeeId, "Student Fee ID")
        return feePaymentRepository.deleteByStudentFeeId(studentFeeId)
    }

    suspend fun deletePaymentsByStudentId(studentId: String): Int {
        validateUUID(studentId, "Student ID")
        return feePaymentRepository.deleteByStudentId(studentId)
    }

    private fun validateCreateRequest(request: CreateFeePaymentRequest) {
        if (request.studentFeeId.isBlank()) {
            throw ApiException("Student fee ID is required", HttpStatusCode.BadRequest)
        }

        validateUUID(request.studentFeeId, "Student Fee ID")

        if (request.amount.isBlank()) {
            throw ApiException("Amount is required", HttpStatusCode.BadRequest)
        }

        val amount = try {
            BigDecimal(request.amount)
        } catch (e: NumberFormatException) {
            throw ApiException("Invalid amount format", HttpStatusCode.BadRequest)
        }

        if (amount <= BigDecimal.ZERO) {
            throw ApiException("Amount must be greater than zero", HttpStatusCode.BadRequest)
        }

        if (request.paymentMode.isBlank()) {
            throw ApiException("Payment mode is required", HttpStatusCode.BadRequest)
        }

        try {
            PaymentMode.valueOf(request.paymentMode.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid payment mode: ${request.paymentMode}", HttpStatusCode.BadRequest)
        }

        request.receiptNumber?.let { receiptNumber ->
            if (receiptNumber.length > 100) {
                throw ApiException("Receipt number cannot exceed 100 characters", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun validateUpdateRequest(request: UpdateFeePaymentRequest) {
        if (request.amount.isBlank()) {
            throw ApiException("Amount is required", HttpStatusCode.BadRequest)
        }

        val amount = try {
            BigDecimal(request.amount)
        } catch (e: NumberFormatException) {
            throw ApiException("Invalid amount format", HttpStatusCode.BadRequest)
        }

        if (amount <= BigDecimal.ZERO) {
            throw ApiException("Amount must be greater than zero", HttpStatusCode.BadRequest)
        }

        if (request.paymentMode.isBlank()) {
            throw ApiException("Payment mode is required", HttpStatusCode.BadRequest)
        }

        try {
            PaymentMode.valueOf(request.paymentMode.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid payment mode: ${request.paymentMode}", HttpStatusCode.BadRequest)
        }

        request.receiptNumber?.let { receiptNumber ->
            if (receiptNumber.length > 100) {
                throw ApiException("Receipt number cannot exceed 100 characters", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun validateUUID(id: String, fieldName: String) {
        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid $fieldName format", HttpStatusCode.BadRequest)
        }
    }

    private fun parseDateTime(dateTimeString: String, fieldName: String): LocalDateTime {
        return try {
            LocalDateTime.parse(dateTimeString)
        } catch (e: DateTimeParseException) {
            throw ApiException("Invalid $fieldName format. Expected format: yyyy-MM-ddTHH:mm:ss", HttpStatusCode.BadRequest)
        }
    }

    private fun parseDate(dateString: String, fieldName: String): LocalDate {
        return try {
            LocalDate.parse(dateString)
        } catch (e: DateTimeParseException) {
            throw ApiException("Invalid $fieldName format. Expected format: yyyy-MM-dd", HttpStatusCode.BadRequest)
        }
    }
}