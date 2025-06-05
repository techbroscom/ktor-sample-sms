package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDateTime
import java.util.*

class ExamScheduleRepository {

    suspend fun create(request: CreateExamScheduleRequest): String = dbQuery {
        ExamSchedules.insert {
            it[examId] = UUID.fromString(request.examId)
            it[classId] = UUID.fromString(request.classId)
            it[startTime] = LocalDateTime.parse(request.startTime)
            it[endTime] = LocalDateTime.parse(request.endTime)
        }[ExamSchedules.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateExamScheduleRequest>): List<String> = dbQuery {
        requests.map { request ->
            ExamSchedules.insert {
                it[examId] = UUID.fromString(request.examId)
                it[classId] = UUID.fromString(request.classId)
                it[startTime] = LocalDateTime.parse(request.startTime)
                it[endTime] = LocalDateTime.parse(request.endTime)
            }[ExamSchedules.id].toString()
        }
    }

    suspend fun findById(id: String): ExamScheduleDto? = dbQuery {
        ExamSchedules
            .join(Exams, JoinType.LEFT, ExamSchedules.examId, Exams.id)
            .join(Classes, JoinType.LEFT, ExamSchedules.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ExamSchedules.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<ExamScheduleDto> = dbQuery {
        ExamSchedules
            .join(Exams, JoinType.LEFT, ExamSchedules.examId, Exams.id)
            .join(Classes, JoinType.LEFT, ExamSchedules.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(ExamSchedules.startTime to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateExamScheduleRequest): Boolean = dbQuery {
        ExamSchedules.update({ ExamSchedules.id eq UUID.fromString(id) }) {
            it[examId] = UUID.fromString(request.examId)
            it[classId] = UUID.fromString(request.classId)
            it[startTime] = LocalDateTime.parse(request.startTime)
            it[endTime] = LocalDateTime.parse(request.endTime)
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        ExamSchedules.deleteWhere { ExamSchedules.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByExamId(examId: String): List<ExamScheduleDto> = dbQuery {
        ExamSchedules
            .join(Exams, JoinType.LEFT, ExamSchedules.examId, Exams.id)
            .join(Classes, JoinType.LEFT, ExamSchedules.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ExamSchedules.examId eq UUID.fromString(examId) }
            .orderBy(ExamSchedules.startTime to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassId(classId: String): List<ExamScheduleDto> = dbQuery {
        ExamSchedules
            .join(Exams, JoinType.LEFT, ExamSchedules.examId, Exams.id)
            .join(Classes, JoinType.LEFT, ExamSchedules.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ExamSchedules.classId eq UUID.fromString(classId) }
            .orderBy(ExamSchedules.startTime to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByDateRange(startDate: LocalDateTime, endDate: LocalDateTime): List<ExamScheduleDto> = dbQuery {
        ExamSchedules
            .join(Exams, JoinType.LEFT, ExamSchedules.examId, Exams.id)
            .join(Classes, JoinType.LEFT, ExamSchedules.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (ExamSchedules.startTime greaterEq startDate) and
                        (ExamSchedules.endTime lessEq endDate)
            }
            .orderBy(ExamSchedules.startTime to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(examId: String, classId: String, excludeId: String? = null): Boolean = dbQuery {
        val query = ExamSchedules.selectAll()
            .where {
                (ExamSchedules.examId eq UUID.fromString(examId)) and
                        (ExamSchedules.classId eq UUID.fromString(classId))
            }

        if (excludeId != null) {
            query.andWhere { ExamSchedules.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun checkTimeConflict(
        classId: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
        excludeId: String? = null
    ): Boolean = dbQuery {
        val query = ExamSchedules.selectAll()
            .where {
                (ExamSchedules.classId eq UUID.fromString(classId)) and
                        (
                                ((ExamSchedules.startTime lessEq startTime) and (ExamSchedules.endTime greater startTime)) or
                                        ((ExamSchedules.startTime less endTime) and (ExamSchedules.endTime greaterEq endTime)) or
                                        ((ExamSchedules.startTime greaterEq startTime) and (ExamSchedules.endTime lessEq endTime))
                                )
            }

        if (excludeId != null) {
            query.andWhere { ExamSchedules.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByExamId(examId: String): Int = dbQuery {
        ExamSchedules.deleteWhere { ExamSchedules.examId eq UUID.fromString(examId) }
    }

    suspend fun deleteByClassId(classId: String): Int = dbQuery {
        ExamSchedules.deleteWhere { ExamSchedules.classId eq UUID.fromString(classId) }
    }

    suspend fun getExamsWithSchedules(academicYearId: String): List<ExamWithSchedulesDto> = dbQuery {
        val examSchedules = ExamSchedules
            .join(Exams, JoinType.INNER, ExamSchedules.examId, Exams.id)
            .join(Classes, JoinType.INNER, ExamSchedules.classId, Classes.id)
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Exams.name to SortOrder.ASC, ExamSchedules.startTime to SortOrder.ASC)
            .toList()

        examSchedules.groupBy {
            ExamInfo(
                it[ExamSchedules.examId].toString(),
                it[Exams.name],
                it[Subjects.name],
                it[Subjects.code],
                it[Exams.maxMarks],
                it[Exams.date].toString(),
                it[AcademicYears.year] ?: ""
            )
        }.map { (examInfo, rows) ->
            ExamWithSchedulesDto(
                examId = examInfo.id,
                examName = examInfo.name,
                subjectName = examInfo.subjectName,
                subjectCode = examInfo.subjectCode,
                maxMarks = examInfo.maxMarks,
                examDate = examInfo.date,
                academicYearName = examInfo.academicYearName,
                schedules = rows.map { row ->
                    ScheduleDto(
                        id = row[ExamSchedules.id].toString(),
                        classId = row[ExamSchedules.classId].toString(),
                        className = row[Classes.className],
                        sectionName = row[Classes.sectionName],
                        startTime = row[ExamSchedules.startTime].toString(),
                        endTime = row[ExamSchedules.endTime].toString()
                    )
                }
            )
        }
    }

    suspend fun getClassesWithExamSchedules(academicYearId: String): List<ClassWithExamSchedulesDto> = dbQuery {
        val examSchedules = ExamSchedules
            .join(Exams, JoinType.INNER, ExamSchedules.examId, Exams.id)
            .join(Classes, JoinType.INNER, ExamSchedules.classId, Classes.id)
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC, ExamSchedules.startTime to SortOrder.ASC)
            .toList()

        examSchedules.groupBy {
            ClassInfo(
                it[ExamSchedules.classId].toString(),
                it[Classes.className],
                it[Classes.sectionName],
                it[AcademicYears.year] ?: ""
            )
        }.map { (classInfo, rows) ->
            ClassWithExamSchedulesDto(
                classId = classInfo.id,
                className = classInfo.name,
                sectionName = classInfo.sectionName,
                academicYearName = classInfo.academicYearName,
                schedules = rows.map { row ->
                    ExamScheduleDetailDto(
                        id = row[ExamSchedules.id].toString(),
                        examId = row[ExamSchedules.examId].toString(),
                        examName = row[Exams.name],
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code],
                        maxMarks = row[Exams.maxMarks],
                        examDate = row[Exams.date].toString(),
                        startTime = row[ExamSchedules.startTime].toString(),
                        endTime = row[ExamSchedules.endTime].toString()
                    )
                }
            )
        }
    }

    private fun mapRowToDto(row: ResultRow): ExamScheduleDto {
        return ExamScheduleDto(
            id = row[ExamSchedules.id].toString(),
            examId = row[ExamSchedules.examId].toString(),
            classId = row[ExamSchedules.classId].toString(),
            startTime = row[ExamSchedules.startTime].toString(),
            endTime = row[ExamSchedules.endTime].toString(),
            examName = row.getOrNull(Exams.name),
            subjectName = row.getOrNull(Subjects.name),
            subjectCode = row.getOrNull(Subjects.code),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            maxMarks = row.getOrNull(Exams.maxMarks),
            examDate = row.getOrNull(Exams.date)?.toString(),
            academicYearName = row.getOrNull(AcademicYears.year)
        )
    }

    // Helper data classes for grouping
    private data class ExamInfo(
        val id: String,
        val name: String,
        val subjectName: String,
        val subjectCode: String?,
        val maxMarks: Int,
        val date: String,
        val academicYearName: String
    )

    private data class ClassInfo(
        val id: String,
        val name: String,
        val sectionName: String,
        val academicYearName: String
    )
}