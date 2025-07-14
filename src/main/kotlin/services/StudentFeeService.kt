package com.example.services

import com.example.database.tables.FeeStatus
import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.StudentFeeRepository
import com.example.repositories.FeePaymentRepository
import io.ktor.http.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

class StudentFeeService(
    private val studentFeeRepository: StudentFeeRepository,
    private val feePaymentRepository: FeePaymentRepository,
    private val userService: UserService,
    private val feesStructureService: FeesStructureService
) {

    suspend fun createStudentFee(request: CreateStudentFeeRequest): StudentFeeDto {
        validateCreateRequest(request)

        // Validate that referenced entities exist
        userService.getUserById(request.studentId)
        feesStructureService.getFeesStructureById(request.feeStructureId)

        // Check for duplicate
        val isDuplicate = studentFeeRepository.checkDuplicate(
            request.studentId,
            request.feeStructureId,
            request.month
        )
        if (isDuplicate) {
            throw ApiException(
                "Fee record already exists for this student, fee structure, and month",
                HttpStatusCode.Conflict
            )
        }

        val studentFeeId = studentFeeRepository.create(request)
        return getStudentFeeById(studentFeeId)
    }

    suspend fun bulkCreateStudentFees(request: BulkCreateStudentFeeRequest): List<StudentFeeDto> {
        validateBulkCreateRequest(request)

        // Validate that referenced entities exist
        val feeStructure = feesStructureService.getFeesStructureById(request.feeStructureId)

        if (request.studentIds.isEmpty()) {
            throw ApiException("Student IDs list cannot be empty", HttpStatusCode.BadRequest)
        }

        val createRequests = mutableListOf<CreateStudentFeeRequest>()

        for (studentId in request.studentIds) {
            validateUUID(studentId, "Student ID")
            userService.getUserById(studentId)

            // Check for duplicate
            val isDuplicate = studentFeeRepository.checkDuplicate(
                studentId,
                request.feeStructureId,
                request.month
            )
            if (!isDuplicate) {
                createRequests.add(CreateStudentFeeRequest(
                    studentId = studentId,
                    feeStructureId = request.feeStructureId,
                    amount = feeStructure.amount,
                    dueDate = request.dueDate,
                    month = request.month
                ))
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException("All students already have fee records for this month", HttpStatusCode.Conflict)
        }

        val studentFeeIds = studentFeeRepository.bulkCreate(createRequests)
        return studentFeeIds.map { getStudentFeeById(it) }
    }

    suspend fun getStudentFeeById(id: String): StudentFeeDto {
        validateUUID(id, "Student Fee ID")
        return studentFeeRepository.findById(id)
            ?: throw ApiException("Student Fee record not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllStudentFees(): List<StudentFeeDto> {
        return studentFeeRepository.findAll()
    }

    suspend fun updateStudentFee(id: String, request: UpdateStudentFeeRequest): StudentFeeDto {
        validateUUID(id, "Student Fee ID")
        validateUpdateRequest(request)

        val updated = studentFeeRepository.update(id, request)
        if (!updated) {
            throw ApiException("Student Fee record not found", HttpStatusCode.NotFound)
        }

        return getStudentFeeById(id)
    }

    suspend fun deleteStudentFee(id: String) {
        validateUUID(id, "Student Fee ID")
        val deleted = studentFeeRepository.delete(id)
        if (!deleted) {
            throw ApiException("Student Fee record not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getStudentFeesByStudentId(studentId: String): List<StudentFeeDto> {
        validateUUID(studentId, "Student ID")
        userService.getUserById(studentId)
        return studentFeeRepository.findByStudentId(studentId)
    }

    suspend fun getStudentFeesByFeeStructureId(feeStructureId: String): List<StudentFeeDto> {
        validateUUID(feeStructureId, "Fee Structure ID")
        feesStructureService.getFeesStructureById(feeStructureId)
        return studentFeeRepository.findByFeeStructureId(feeStructureId)
    }

    suspend fun getStudentFeesByMonth(month: String): List<StudentFeeDto> {
        validateMonth(month)
        return studentFeeRepository.findByMonth(month)
    }

    suspend fun getStudentFeesByStatus(status: String): List<StudentFeeDto> {
        val feeStatus = try {
            FeeStatus.valueOf(status.uppercase())
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid fee status: $status", HttpStatusCode.BadRequest)
        }
        return studentFeeRepository.findByStatus(feeStatus)
    }

    suspend fun getOverdueFees(): List<StudentFeeDto> {
        return studentFeeRepository.findOverdueFees()
    }

    suspend fun getStudentFeesByClassAndMonth(classId: String, month: String): List<StudentFeeDto> {
        validateUUID(classId, "Class ID")
        validateMonth(month)
        return studentFeeRepository.findByClassAndMonth(classId, month)
    }

    suspend fun payFee(id: String, request: PayFeeRequest): StudentFeeDto {
        validateUUID(id, "Student Fee ID")
        validatePayFeeRequest(request)

        val studentFee = getStudentFeeById(id)
        val currentPaidAmount = BigDecimal(studentFee.paidAmount)
        val paymentAmount = BigDecimal(request.amount)
        val totalAmount = BigDecimal(studentFee.amount)
        val newPaidAmount = currentPaidAmount + paymentAmount

        if (newPaidAmount > totalAmount) {
            throw ApiException("Payment amount exceeds the remaining balance", HttpStatusCode.BadRequest)
        }

        // Update the student fee record
        studentFeeRepository.updatePayment(id, newPaidAmount)

        // Create payment record
        val paymentRequest = CreateFeePaymentRequest(
            studentFeeId = id,
            amount = request.amount,
            paymentMode = request.paymentMode,
            receiptNumber = request.receiptNumber,
            remarks = request.remarks
        )
        feePaymentRepository.create(paymentRequest)

        return getStudentFeeById(id)
    }

    suspend fun getStudentFeesSummary(studentId: String): StudentFeesSummaryDto {
        validateUUID(studentId, "Student ID")
        userService.getUserById(studentId)
        return studentFeeRepository.getStudentFeesSummary(studentId)
            ?: throw ApiException("No fee records found for this student", HttpStatusCode.NotFound)
    }

    suspend fun getMonthlyFeeReport(month: String): MonthlyFeeReportDto {
        validateMonth(month)
        return studentFeeRepository.getMonthlyFeeReport(month)
    }

    suspend fun getClassFeesSummary(classId: String, month: String): ClassFeesSummaryDto {
        validateUUID(classId, "Class ID")
        validateMonth(month)
        return studentFeeRepository.getClassFeesSummary(classId, month)
            ?: throw ApiException("No fee records found for this class and month", HttpStatusCode.NotFound)
    }

    suspend fun removeStudentFees(studentId: String): Int {
        validateUUID(studentId, "Student ID")
        userService.getUserById(studentId)
        return studentFeeRepository.deleteByStudentId(studentId)
    }

    suspend fun removeFeesByStructure(feeStructureId: String): Int {
        validateUUID(feeStructureId, "Fee Structure ID")
        feesStructureService.getFeesStructureById(feeStructureId)
        return studentFeeRepository.deleteByFeeStructureId(feeStructureId)
    }

    // Validation methods
    private fun validateCreateRequest(request: CreateStudentFeeRequest) {
        validateUUID(request.studentId, "Student ID")
        validateUUID(request.feeStructureId, "Fee Structure ID")
        validateAmount(request.amount)
        validateDate(request.dueDate, "Due Date")
        validateMonth(request.month)
    }

    private fun validateBulkCreateRequest(request: BulkCreateStudentFeeRequest) {
        validateUUID(request.feeStructureId, "Fee Structure ID")
        validateDate(request.dueDate, "Due Date")
        validateMonth(request.month)

        if (request.studentIds.isEmpty()) {
            throw ApiException("Student IDs list cannot be empty", HttpStatusCode.BadRequest)
        }
    }

    private fun validateUpdateRequest(request: UpdateStudentFeeRequest) {
        validateAmount(request.amount)
        validateDate(request.dueDate, "Due Date")
        validateMonth(request.month)
    }

    private fun validatePayFeeRequest(request: PayFeeRequest) {
        validateAmount(request.amount)

        if (request.paymentMode.isBlank()) {
            throw ApiException("Payment mode cannot be empty", HttpStatusCode.BadRequest)
        }

        if (request.paymentMode.length > 50) {
            throw ApiException("Payment mode cannot exceed 50 characters", HttpStatusCode.BadRequest)
        }

        request.receiptNumber?.let {
            if (it.length > 100) {
                throw ApiException("Receipt number cannot exceed 100 characters", HttpStatusCode.BadRequest)
            }
        }

        request.remarks?.let {
            if (it.length > 500) {
                throw ApiException("Remarks cannot exceed 500 characters", HttpStatusCode.BadRequest)
            }
        }
    }

    private fun validateUUID(id: String, fieldName: String) {
        if (id.isBlank()) {
            throw ApiException("$fieldName cannot be empty", HttpStatusCode.BadRequest)
        }

        try {
            UUID.fromString(id)
        } catch (e: IllegalArgumentException) {
            throw ApiException("Invalid $fieldName format", HttpStatusCode.BadRequest)
        }
    }

    private fun validateAmount(amount: String) {
        if (amount.isBlank()) {
            throw ApiException("Amount cannot be empty", HttpStatusCode.BadRequest)
        }

        val amountValue = try {
            BigDecimal(amount)
        } catch (e: NumberFormatException) {
            throw ApiException("Invalid amount format", HttpStatusCode.BadRequest)
        }

        if (amountValue <= BigDecimal.ZERO) {
            throw ApiException("Amount must be greater than zero", HttpStatusCode.BadRequest)
        }

        if (amountValue.scale() > 2) {
            throw ApiException("Amount cannot have more than 2 decimal places", HttpStatusCode.BadRequest)
        }

        if (amountValue >= BigDecimal("10000000")) {
            throw ApiException("Amount cannot exceed 9,999,999.99", HttpStatusCode.BadRequest)
        }
    }

    private fun validateDate(date: String, fieldName: String) {
        if (date.isBlank()) {
            throw ApiException("$fieldName cannot be empty", HttpStatusCode.BadRequest)
        }

        try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            throw ApiException("Invalid $fieldName format. Expected format: YYYY-MM-DD", HttpStatusCode.BadRequest)
        }
    }

    private fun validateMonth(month: String) {
        if (month.isBlank()) {
            throw ApiException("Month cannot be empty", HttpStatusCode.BadRequest)
        }

        val monthPattern = Regex("^\\d{4}-\\d{2}$")
        if (!monthPattern.matches(month)) {
            throw ApiException("Invalid month format. Expected format: YYYY-MM", HttpStatusCode.BadRequest)
        }

        try {
            val parts = month.split("-")
            val year = parts[0].toInt()
            val monthValue = parts[1].toInt()

            if (year < 1900 || year > 2100) {
                throw ApiException("Year must be between 1900 and 2100", HttpStatusCode.BadRequest)
            }

            if (monthValue < 1 || monthValue > 12) {
                throw ApiException("Month must be between 01 and 12", HttpStatusCode.BadRequest)
            }
        } catch (e: NumberFormatException) {
            throw ApiException("Invalid month format. Expected format: YYYY-MM", HttpStatusCode.BadRequest)
        }
    }
}