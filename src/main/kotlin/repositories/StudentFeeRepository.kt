package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class StudentFeeRepository {

    suspend fun create(request: CreateStudentFeeRequest): String = dbQuery {
        StudentFees.insert {
            it[studentId] = UUID.fromString(request.studentId)
            it[feeStructureId] = UUID.fromString(request.feeStructureId)
            it[amount] = BigDecimal(request.amount)
            it[paidAmount] = BigDecimal.ZERO
            it[status] = FeeStatus.PENDING
            it[dueDate] = LocalDate.parse(request.dueDate)
            it[month] = request.month
            it[createdAt] = LocalDateTime.now()
            it[updatedAt] = LocalDateTime.now()
        }[StudentFees.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateStudentFeeRequest>): List<String> = dbQuery {
        requests.map { request ->
            StudentFees.insert {
                it[studentId] = UUID.fromString(request.studentId)
                it[feeStructureId] = UUID.fromString(request.feeStructureId)
                it[amount] = BigDecimal(request.amount)
                it[paidAmount] = BigDecimal.ZERO
                it[status] = FeeStatus.PENDING
                it[dueDate] = LocalDate.parse(request.dueDate)
                it[month] = request.month
                it[createdAt] = LocalDateTime.now()
                it[updatedAt] = LocalDateTime.now()
            }[StudentFees.id].toString()
        }
    }

    suspend fun findById(id: String): StudentFeeDto? = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentFees.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<StudentFeeDto> = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(StudentFees.createdAt to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateStudentFeeRequest): Boolean = dbQuery {
        StudentFees.update({ StudentFees.id eq UUID.fromString(id) }) {
            it[amount] = BigDecimal(request.amount)
            it[dueDate] = LocalDate.parse(request.dueDate)
            it[month] = request.month
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        StudentFees.deleteWhere { StudentFees.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByStudentId(studentId: String): List<StudentFeeDto> = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentFees.studentId eq UUID.fromString(studentId) }
            .orderBy(StudentFees.dueDate to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByFeeStructureId(feeStructureId: String): List<StudentFeeDto> = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentFees.feeStructureId eq UUID.fromString(feeStructureId) }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByMonth(month: String): List<StudentFeeDto> = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentFees.month eq month }
            .orderBy(Classes.className to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStatus(status: FeeStatus): List<StudentFeeDto> = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentFees.status eq status }
            .orderBy(StudentFees.dueDate to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findOverdueFees(): List<StudentFeeDto> = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (StudentFees.status neq FeeStatus.PAID) and
                        (StudentFees.dueDate less LocalDate.now())
            }
            .orderBy(StudentFees.dueDate to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndMonth(classId: String, month: String): List<StudentFeeDto> = dbQuery {
        StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, FeesStructures.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (StudentFees.month eq month)
            }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(studentId: String, feeStructureId: String, month: String, excludeId: String? = null): Boolean = dbQuery {
        val query = StudentFees.selectAll()
            .where {
                (StudentFees.studentId eq UUID.fromString(studentId)) and
                        (StudentFees.feeStructureId eq UUID.fromString(feeStructureId)) and
                        (StudentFees.month eq month)
            }

        if (excludeId != null) {
            query.andWhere { StudentFees.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun updatePayment(id: String, paidAmount: BigDecimal): Boolean = dbQuery {
        val currentFee = StudentFees.selectAll()
            .where { StudentFees.id eq UUID.fromString(id) }
            .singleOrNull()

        if (currentFee != null) {
            val totalAmount = currentFee[StudentFees.amount]
            val newStatus = when {
                paidAmount >= totalAmount -> FeeStatus.PAID
                paidAmount > BigDecimal.ZERO -> FeeStatus.PARTIALLY_PAID
                else -> FeeStatus.PENDING
            }

            val today = if (newStatus == FeeStatus.PAID) LocalDate.now() else null

            StudentFees.update({ StudentFees.id eq UUID.fromString(id) }) {
                it[StudentFees.paidAmount] = paidAmount
                it[status] = newStatus
                if (today != null) {
                    it[paidDate] = today
                }
                it[updatedAt] = LocalDateTime.now()
            } > 0
        } else {
            false
        }
    }

    suspend fun getStudentFeesSummary(studentId: String): StudentFeesSummaryDto? = dbQuery {
        val fees = StudentFees
            .join(Users, JoinType.LEFT, StudentFees.studentId, Users.id)
            .selectAll()
            .where { StudentFees.studentId eq UUID.fromString(studentId) }
            .toList()

        if (fees.isEmpty()) {
            null
        } else {
            val totalFees = fees.sumOf { it[StudentFees.amount] }
            val totalPaid = fees.sumOf { it[StudentFees.paidAmount] }
            val totalBalance = totalFees - totalPaid

            val paidCount = fees.count { it[StudentFees.status] == FeeStatus.PAID }
            val partiallyPaidCount = fees.count { it[StudentFees.status] == FeeStatus.PARTIALLY_PAID }
            val pendingCount = fees.count { it[StudentFees.status] == FeeStatus.PENDING }
            val overdueCount = fees.count {
                it[StudentFees.status] != FeeStatus.PAID &&
                        it[StudentFees.dueDate] < LocalDate.now()
            }

            StudentFeesSummaryDto(
                studentId = studentId,
                studentName = "${fees.first()[Users.firstName]} ${fees.first()[Users.lastName]}",
                totalFees = totalFees.toString(),
                totalPaid = totalPaid.toString(),
                totalBalance = totalBalance.toString(),
                paidCount = paidCount,
                partiallyPaidCount = partiallyPaidCount,
                pendingCount = pendingCount,
                overdueCount = overdueCount
            )
        }
    }

    suspend fun getMonthlyFeeReport(month: String): MonthlyFeeReportDto = dbQuery {
        val fees = StudentFees
            .selectAll()
            .where { StudentFees.month eq month }
            .toList()

        val totalFees = fees.sumOf { it[StudentFees.amount] }
        val totalPaid = fees.sumOf { it[StudentFees.paidAmount] }
        val totalBalance = totalFees - totalPaid

        val paidCount = fees.count { it[StudentFees.status] == FeeStatus.PAID }
        val partiallyPaidCount = fees.count { it[StudentFees.status] == FeeStatus.PARTIALLY_PAID }
        val pendingCount = fees.count { it[StudentFees.status] == FeeStatus.PENDING }

        MonthlyFeeReportDto(
            month = month,
            totalFees = totalFees.toString(),
            totalPaid = totalPaid.toString(),
            totalBalance = totalBalance.toString(),
            studentCount = fees.size,
            paidCount = paidCount,
            partiallyPaidCount = partiallyPaidCount,
            pendingCount = pendingCount
        )
    }

    suspend fun getClassFeesSummary(classId: String, month: String): ClassFeesSummaryDto? = dbQuery {
        val fees = StudentFees
            .join(FeesStructures, JoinType.LEFT, StudentFees.feeStructureId, FeesStructures.id)
            .join(Classes, JoinType.LEFT, FeesStructures.classId, Classes.id)
            .selectAll()
            .where {
                (FeesStructures.classId eq UUID.fromString(classId)) and
                        (StudentFees.month eq month)
            }
            .toList()

        if (fees.isEmpty()) {
            null
        } else {
            val totalFees = fees.sumOf { it[StudentFees.amount] }
            val totalPaid = fees.sumOf { it[StudentFees.paidAmount] }
            val totalBalance = totalFees - totalPaid
            val collectionPercentage = if (totalFees > BigDecimal.ZERO) {
                ((totalPaid.toDouble() / totalFees.toDouble()) * 100).toString()
            } else {
                "0"
            }

            ClassFeesSummaryDto(
                classId = classId,
                className = fees.first()[Classes.className],
                sectionName = fees.first()[Classes.sectionName],
                totalFees = totalFees.toString(),
                totalPaid = totalPaid.toString(),
                totalBalance = totalBalance.toString(),
                studentCount = fees.size,
                collectionPercentage = collectionPercentage
            )
        }
    }

    suspend fun deleteByStudentId(studentId: String): Int = dbQuery {
        StudentFees.deleteWhere { StudentFees.studentId eq UUID.fromString(studentId) }
    }

    suspend fun deleteByFeeStructureId(feeStructureId: String): Int = dbQuery {
        StudentFees.deleteWhere { StudentFees.feeStructureId eq UUID.fromString(feeStructureId) }
    }

    private fun mapRowToDto(row: ResultRow): StudentFeeDto {
        val amount = row[StudentFees.amount]
        val paidAmount = row[StudentFees.paidAmount]
        val balanceAmount = amount - paidAmount

        return StudentFeeDto(
            id = row[StudentFees.id].toString(),
            studentId = row[StudentFees.studentId].toString(),
            feeStructureId = row[StudentFees.feeStructureId].toString(),
            amount = amount.toString(),
            paidAmount = paidAmount.toString(),
            status = row[StudentFees.status].name,
            dueDate = row[StudentFees.dueDate].toString(),
            paidDate = row[StudentFees.paidDate]?.toString(),
            month = row[StudentFees.month],
            createdAt = row[StudentFees.createdAt].toString(),
            updatedAt = row[StudentFees.updatedAt].toString(),
            studentName = row.getOrNull(Users.firstName)?.let { firstName ->
                "${firstName} ${row.getOrNull(Users.lastName) ?: ""}"
            },
            studentEmail = row.getOrNull(Users.email),
            feeStructureName = row.getOrNull(FeesStructures.name),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year),
            balanceAmount = balanceAmount.toString()
        )
    }
}