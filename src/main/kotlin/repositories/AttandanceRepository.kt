package com.example.repositories

import com.example.database.tables.Attendance
import com.example.database.tables.AttendanceStatus
import com.example.database.tables.Classes
import com.example.database.tables.Users
import com.example.models.dto.*
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.util.*

class AttendanceRepository {

    suspend fun create(request: CreateAttendanceRequest): String = dbQuery {
        Attendance.insert {
            it[studentId] = UUID.fromString(request.studentId)
            it[classId] = UUID.fromString(request.classId)
            it[date] = LocalDate.parse(request.date)
            it[status] = request.status
        }[Attendance.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateAttendanceRequest>): List<String> = dbQuery {
        requests.map { request ->
            Attendance.insert {
                it[studentId] = UUID.fromString(request.studentId)
                it[classId] = UUID.fromString(request.classId)
                it[date] = LocalDate.parse(request.date)
                it[status] = request.status
            }[Attendance.id].toString()
        }
    }

    suspend fun findById(id: String): AttendanceDto? = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where { Attendance.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<AttendanceDto> = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .orderBy(Attendance.date to SortOrder.DESC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateAttendanceRequest): Boolean = dbQuery {
        Attendance.update({ Attendance.id eq UUID.fromString(id) }) {
            it[studentId] = UUID.fromString(request.studentId)
            it[classId] = UUID.fromString(request.classId)
            it[date] = LocalDate.parse(request.date)
            it[status] = request.status
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        Attendance.deleteWhere { Attendance.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByStudentId(studentId: String): List<AttendanceDto> = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where { Attendance.studentId eq UUID.fromString(studentId) }
            .orderBy(Attendance.date to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassId(classId: String): List<AttendanceDto> = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where { Attendance.classId eq UUID.fromString(classId) }
            .orderBy(Attendance.date to SortOrder.DESC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByDate(date: LocalDate): List<AttendanceDto> = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where { Attendance.date eq date }
            .orderBy(Classes.className to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndDate(classId: String, date: LocalDate): List<AttendanceDto> = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where {
                (Attendance.classId eq UUID.fromString(classId)) and
                        (Attendance.date eq date)
            }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStudentAndDateRange(studentId: String, startDate: LocalDate, endDate: LocalDate): List<AttendanceDto> = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where {
                (Attendance.studentId eq UUID.fromString(studentId)) and
                        (Attendance.date greaterEq startDate) and
                        (Attendance.date lessEq endDate)
            }
            .orderBy(Attendance.date to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndDateRange(classId: String, startDate: LocalDate, endDate: LocalDate): List<AttendanceDto> = dbQuery {
        Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where {
                (Attendance.classId eq UUID.fromString(classId)) and
                        (Attendance.date greaterEq startDate) and
                        (Attendance.date lessEq endDate)
            }
            .orderBy(Attendance.date to SortOrder.DESC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(studentId: String, classId: String, date: LocalDate, excludeId: String? = null): Boolean = dbQuery {
        val query = Attendance.selectAll()
            .where {
                (Attendance.studentId eq UUID.fromString(studentId)) and
                        (Attendance.classId eq UUID.fromString(classId)) and
                        (Attendance.date eq date)
            }

        if (excludeId != null) {
            query.andWhere { Attendance.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByStudentId(studentId: String): Int = dbQuery {
        Attendance.deleteWhere { Attendance.studentId eq UUID.fromString(studentId) }
    }

    suspend fun deleteByClassId(classId: String): Int = dbQuery {
        Attendance.deleteWhere { Attendance.classId eq UUID.fromString(classId) }
    }

    suspend fun deleteByDate(date: LocalDate): Int = dbQuery {
        Attendance.deleteWhere { Attendance.date eq date }
    }

    suspend fun getAttendanceStats(studentId: String, startDate: LocalDate, endDate: LocalDate): AttendanceStatsDto = dbQuery {
        val records = Attendance.selectAll()
            .where {
                (Attendance.studentId eq UUID.fromString(studentId)) and
                        (Attendance.date greaterEq startDate) and
                        (Attendance.date lessEq endDate)
            }
            .toList()

        val totalDays = records.size
        val presentDays = records.count { it[Attendance.status] == AttendanceStatus.PRESENT }
        val absentDays = records.count { it[Attendance.status] == AttendanceStatus.ABSENT }
        val lateDays = records.count { it[Attendance.status] == AttendanceStatus.LATE }
        val attendancePercentage = if (totalDays > 0) (presentDays.toDouble() / totalDays) * 100 else 0.0

        AttendanceStatsDto(
            totalDays = totalDays,
            presentDays = presentDays,
            absentDays = absentDays,
            lateDays = lateDays,
            attendancePercentage = attendancePercentage
        )
    }

    suspend fun getClassAttendanceForDate(classId: String, date: LocalDate): ClassAttendanceDto = dbQuery {
        val records = Attendance
            .join(Users, JoinType.LEFT, Attendance.studentId, Users.id)
            .join(Classes, JoinType.LEFT, Attendance.classId, Classes.id)
            .selectAll()
            .where {
                (Attendance.classId eq UUID.fromString(classId)) and
                        (Attendance.date eq date)
            }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }

        val classInfo = Classes.selectAll()
            .where { Classes.id eq UUID.fromString(classId) }
            .single()

        val presentCount = records.count { it.status == AttendanceStatus.PRESENT }
        val absentCount = records.count { it.status == AttendanceStatus.ABSENT }
        val lateCount = records.count { it.status == AttendanceStatus.LATE }

        ClassAttendanceDto(
            classId = classId,
            className = classInfo[Classes.className],
            sectionName = classInfo[Classes.sectionName],
            date = date.toString(),
            totalStudents = records.size,
            presentCount = presentCount,
            absentCount = absentCount,
            lateCount = lateCount,
            attendanceRecords = records
        )
    }

    private fun mapRowToDto(row: ResultRow): AttendanceDto {
        return AttendanceDto(
            id = row[Attendance.id].toString(),
            studentId = row[Attendance.studentId].toString(),
            classId = row[Attendance.classId].toString(),
            date = row[Attendance.date].toString(),
            status = row[Attendance.status],
            studentName = "${row.getOrNull(Users.firstName) ?: ""} ${row.getOrNull(Users.lastName) ?: ""}".trim(),
            studentEmail = row.getOrNull(Users.email),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName)
        )
    }
}