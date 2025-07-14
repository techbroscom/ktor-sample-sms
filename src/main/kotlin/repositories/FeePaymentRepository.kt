package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.dbQuery
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class FeePaymentRepository {

    suspend fun create(request: CreateFeePaymentRequest): String = tenantDbQuery {
        FeePayments.insert {
            it[studentFeeId] = UUID.fromString(request.studentFeeId)
            it[amount] = BigDecimal(request.amount)
            it[paymentMode] = PaymentMode.valueOf(request.paymentMode.uppercase())
            it[paymentDate] = LocalDateTime.now()
            it[receiptNumber] = request.receiptNumber
            it[remarks] = request.remarks
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[FeePayments.id].toString()
    }

    suspend fun findById(id: String): FeePaymentDto? = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.LEFT, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeePayments.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<FeePaymentDto> = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.LEFT, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(FeePayments.paymentDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateFeePaymentRequest): Boolean = tenantDbQuery {
        FeePayments.update({ FeePayments.id eq UUID.fromString(id) }) {
            it[amount] = BigDecimal(request.amount)
            it[paymentMode] = PaymentMode.valueOf(request.paymentMode.uppercase())
            it[receiptNumber] = request.receiptNumber
            it[remarks] = request.remarks
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: String): Boolean = tenantDbQuery {
        FeePayments.deleteWhere { FeePayments.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByStudentFeeId(studentFeeId: String): List<FeePaymentDto> = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.LEFT, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeePayments.studentFeeId eq UUID.fromString(studentFeeId) }
            .orderBy(FeePayments.paymentDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStudentId(studentId: String): List<FeePaymentDto> = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.INNER, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentFees.studentId eq UUID.fromString(studentId) }
            .orderBy(FeePayments.paymentDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByPaymentMode(paymentMode: PaymentMode): List<FeePaymentDto> = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.LEFT, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeePayments.paymentMode eq paymentMode }
            .orderBy(FeePayments.paymentDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<FeePaymentDto> = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.LEFT, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (FeePayments.paymentDate greaterEq startDate) and
                        (FeePayments.paymentDate lessEq endDate)
            }
            .orderBy(FeePayments.paymentDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByMonth(month: String): List<FeePaymentDto> = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.LEFT, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentFees.month eq month }
            .orderBy(FeePayments.paymentDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndDateRange(classId: String, startDate: LocalDateTime, endDate: LocalDateTime): List<FeePaymentDto> = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.INNER, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.INNER, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (FeePayments.paymentDate greaterEq startDate) and
                        (FeePayments.paymentDate lessEq endDate)
            }
            .orderBy(FeePayments.paymentDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByReceiptNumber(receiptNumber: String): FeePaymentDto? = tenantDbQuery {
        FeePayments
            .join(StudentFees, JoinType.LEFT, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { FeePayments.receiptNumber eq receiptNumber }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun checkReceiptNumberExists(receiptNumber: String, excludeId: String? = null): Boolean = tenantDbQuery {
        val query = FeePayments.selectAll()
            .where { FeePayments.receiptNumber eq receiptNumber }

        if (excludeId != null) {
            query.andWhere { FeePayments.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun getPaymentSummary(studentId: String): PaymentSummaryDto? = tenantDbQuery {
        val payments = FeePayments
            .join(StudentFees, JoinType.INNER, FeePayments.studentFeeId, StudentFees.id)
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .selectAll()
            .where { StudentFees.studentId eq UUID.fromString(studentId) }
            .toList()

        if (payments.isEmpty()) {
            null
        } else {
            val totalPayments = payments.sumOf { it[FeePayments.amount] }
            val paymentCount = payments.size
            val lastPaymentDate = payments.maxByOrNull { it[FeePayments.paymentDate] }?.get(FeePayments.paymentDate)
            val averagePaymentAmount = if (paymentCount > 0) totalPayments.divide(BigDecimal(paymentCount), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO

            val paymentModes = payments.groupBy { it[FeePayments.paymentMode] }
                .mapValues { it.value.size }
                .mapKeys { it.key.name }

            PaymentSummaryDto(
                studentId = studentId,
                studentName = "${payments.first()[Users.firstName]} ${payments.first()[Users.lastName]}",
                totalPayments = totalPayments.toString(),
                paymentCount = paymentCount,
                lastPaymentDate = lastPaymentDate?.toString(),
                averagePaymentAmount = averagePaymentAmount.toString(),
                paymentModes = paymentModes
            )
        }
    }

    suspend fun getDailyPaymentReport(date: LocalDate): DailyPaymentReportDto = tenantDbQuery {
        val startOfDay = date.atStartOfDay()
        val endOfDay = date.atTime(23, 59, 59)

        val payments = FeePayments
            .selectAll()
            .where {
                (FeePayments.paymentDate greaterEq startOfDay) and
                        (FeePayments.paymentDate lessEq endOfDay)
            }
            .toList()

        val totalAmount = payments.sumOf { it[FeePayments.amount] }
        val paymentCount = payments.size

        val paymentModes = payments.groupBy { it[FeePayments.paymentMode] }
            .mapValues { it.value.sumOf { row -> row[FeePayments.amount] } }
            .mapKeys { it.key.name }

        DailyPaymentReportDto(
            date = date.toString(),
            totalAmount = totalAmount.toString(),
            paymentCount = paymentCount,
            paymentModes = paymentModes.mapValues { it.value.toString() },
            cashAmount = paymentModes[PaymentMode.CASH.name]?.toString() ?: "0",
            onlineAmount = paymentModes[PaymentMode.ONLINE.name]?.toString() ?: "0",
            bankTransferAmount = paymentModes[PaymentMode.BANK_TRANSFER.name]?.toString() ?: "0",
            upiAmount = paymentModes[PaymentMode.UPI.name]?.toString() ?: "0",
            cardAmount = paymentModes[PaymentMode.CARD.name]?.toString() ?: "0"
        )
    }

    suspend fun getMonthlyPaymentReport(month: String): MonthlyPaymentReportDto = tenantDbQuery {
        val payments = FeePayments
            .join(StudentFees, JoinType.INNER, FeePayments.studentFeeId, StudentFees.id)
            .selectAll()
            .where { StudentFees.month eq month }
            .toList()

        val totalAmount = payments.sumOf { it[FeePayments.amount] }
        val paymentCount = payments.size
        val studentCount = payments.distinctBy { it[StudentFees.studentId] }.size
        val averagePaymentAmount = if (paymentCount > 0) totalAmount.divide(BigDecimal(paymentCount), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO

        val paymentModes = payments.groupBy { it[FeePayments.paymentMode] }
            .mapValues { it.value.sumOf { row -> row[FeePayments.amount] } }
            .mapKeys { it.key.name }

        MonthlyPaymentReportDto(
            month = month,
            totalAmount = totalAmount.toString(),
            paymentCount = paymentCount,
            paymentModes = paymentModes.mapValues { it.value.toString() },
            studentCount = studentCount,
            averagePaymentAmount = averagePaymentAmount.toString()
        )
    }

    suspend fun getClassPaymentSummary(classId: String, startDate: LocalDateTime, endDate: LocalDateTime): ClassPaymentSummaryDto? = tenantDbQuery {
        val payments = FeePayments
            .join(StudentFees, JoinType.INNER, FeePayments.studentFeeId, StudentFees.id)
            .join(FeesStructures, JoinType.INNER, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (FeePayments.paymentDate greaterEq startDate) and
                        (FeePayments.paymentDate lessEq endDate)
            }
            .toList()

        if (payments.isEmpty()) {
            null
        } else {
            val totalPayments = payments.sumOf { it[FeePayments.amount] }
            val paymentCount = payments.size
            val studentCount = payments.distinctBy { it[StudentFees.studentId] }.size
            val averagePaymentPerStudent = if (studentCount > 0) totalPayments.divide(BigDecimal(studentCount), 2, java.math.RoundingMode.HALF_UP) else BigDecimal.ZERO

            ClassPaymentSummaryDto(
                classId = classId,
                className = payments.first()[Classes.className],
                sectionName = payments.first()[Classes.sectionName],
                totalPayments = totalPayments.toString(),
                paymentCount = paymentCount,
                studentCount = studentCount,
                averagePaymentPerStudent = averagePaymentPerStudent.toString()
            )
        }
    }

    suspend fun deleteByStudentFeeId(studentFeeId: String): Int = tenantDbQuery {
        FeePayments.deleteWhere { FeePayments.studentFeeId eq UUID.fromString(studentFeeId) }
    }

    suspend fun deleteByStudentId(studentId: String): Int = tenantDbQuery {
        val studentFeeIds = StudentFees
            .selectAll()
            .where { StudentFees.studentId eq UUID.fromString(studentId) }
            .map { it[StudentFees.id] }

        if (studentFeeIds.isNotEmpty()) {
            FeePayments.deleteWhere { FeePayments.studentFeeId inList studentFeeIds }
        } else {
            0
        }
    }

    private fun mapRowToDto(row: ResultRow): FeePaymentDto {
        return FeePaymentDto(
            id = row[FeePayments.id].toString(),
            studentFeeId = row[FeePayments.studentFeeId].toString(),
            amount = row[FeePayments.amount].toString(),
            paymentMode = row[FeePayments.paymentMode].name,
            paymentDate = row[FeePayments.paymentDate].toString(),
            receiptNumber = row[FeePayments.receiptNumber],
            remarks = row[FeePayments.remarks],
            createdAt = row[FeePayments.createdAt].toString(),
            updatedAt = row[FeePayments.updatedAt].toString(),
            studentId = row.getOrNull(StudentFees.studentId)?.toString(),
            studentName = row.getOrNull(Users.firstName)?.let { firstName ->
                "${firstName} ${row.getOrNull(Users.lastName) ?: ""}"
            },
            studentEmail = row.getOrNull(Users.email),
            feeStructureId = row.getOrNull(StudentFees.feeStructureId)?.toString(),
            feeStructureName = row.getOrNull(FeesStructures.name),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year),
            month = row.getOrNull(StudentFees.month),
            feeAmount = row.getOrNull(StudentFees.amount)?.toString(),
            feePaidAmount = row.getOrNull(StudentFees.paidAmount)?.toString(),
            feeStatus = row.getOrNull(StudentFees.status)?.name
        )
    }
}