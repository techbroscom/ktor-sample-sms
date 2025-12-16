package com.example.repositories

import com.example.database.tables.AcademicYears
import com.example.database.tables.Classes
import com.example.database.tables.Exams
import com.example.database.tables.ResultStatus
import com.example.database.tables.Subjects
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class ExamRepository {

    suspend fun create(request: CreateExamRequest): String = tenantDbQuery {
        println("Create in Repo Called")
        Exams.insert {
            it[name] = request.name
            it[subjectId] = UUID.fromString(request.subjectId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[maxMarks] = request.maxMarks
            it[date] = LocalDate.parse(request.date)
        }[Exams.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateExamRequest>): List<String> = tenantDbQuery {
        requests.map { request ->
            Exams.insert {
                it[name] = request.name
                it[subjectId] = UUID.fromString(request.subjectId)
                it[classId] = UUID.fromString(request.classId)
                it[academicYearId] = UUID.fromString(request.academicYearId)
                it[maxMarks] = request.maxMarks
                it[date] = LocalDate.parse(request.date)
            }[Exams.id].toString()
        }
    }

    suspend fun findById(id: String): ExamDto? = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Exams.date to SortOrder.ASC, Exams.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateExamRequest): Boolean = tenantDbQuery {
        Exams.update({ Exams.id eq UUID.fromString(id) }) {
            it[name] = request.name
            it[subjectId] = UUID.fromString(request.subjectId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[maxMarks] = request.maxMarks
            it[date] = LocalDate.parse(request.date)
        } > 0
    }

    suspend fun delete(id: String): Boolean = tenantDbQuery {
        Exams.deleteWhere { Exams.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByClassId(classId: String): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.classId eq UUID.fromString(classId) }
            .orderBy(Exams.date to SortOrder.ASC, Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findBySubjectId(subjectId: String): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.subjectId eq UUID.fromString(subjectId) }
            .orderBy(Exams.date to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAcademicYear(academicYearId: String): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Exams.date to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndAcademicYear(classId: String, academicYearId: String): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (Exams.classId eq UUID.fromString(classId)) and
                        (Exams.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Exams.date to SortOrder.ASC, Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findBySubjectAndAcademicYear(subjectId: String, academicYearId: String): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (Exams.subjectId eq UUID.fromString(subjectId)) and
                        (Exams.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Exams.date to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndSubjectAndAcademicYear(
        classId: String,
        subjectId: String,
        academicYearId: String
    ): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (Exams.classId eq UUID.fromString(classId)) and
                        (Exams.subjectId eq UUID.fromString(subjectId)) and
                        (Exams.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Exams.date to SortOrder.ASC, Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByDateRange(startDate: String, endDate: String, academicYearId: String? = null): List<ExamDto> = tenantDbQuery {
        val query = Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                Exams.date.between(LocalDate.parse(startDate), LocalDate.parse(endDate))
            }

        if (academicYearId != null) {
            query.andWhere { Exams.academicYearId eq UUID.fromString(academicYearId) }
        }

        query.orderBy(Exams.date to SortOrder.ASC, Exams.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByDate(date: String): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.date eq LocalDate.parse(date) }
            .orderBy(Exams.name to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(
        name: String,
        classId: String,
        subjectId: String,
        academicYearId: String,
        excludeId: String? = null
    ): Boolean = tenantDbQuery {
        val query = Exams.selectAll()
            .where {
                (Exams.name eq name) and
                        (Exams.classId eq UUID.fromString(classId)) and
                        (Exams.subjectId eq UUID.fromString(subjectId)) and
                        (Exams.academicYearId eq UUID.fromString(academicYearId))
            }

        if (excludeId != null) {
            query.andWhere { Exams.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByClassId(classId: String): Int = tenantDbQuery {
        Exams.deleteWhere { Exams.classId eq UUID.fromString(classId) }
    }

    suspend fun deleteBySubjectId(subjectId: String): Int = tenantDbQuery {
        Exams.deleteWhere { Exams.subjectId eq UUID.fromString(subjectId) }
    }

    suspend fun deleteByAcademicYear(academicYearId: String): Int = tenantDbQuery {
        Exams.deleteWhere { Exams.academicYearId eq UUID.fromString(academicYearId) }
    }

    suspend fun getExamsByClass(academicYearId: String): List<ClassExamsDto> = tenantDbQuery {
        val exams = Exams
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC, Exams.date to SortOrder.ASC)
            .toList()

        exams.groupBy {
            Triple(
                it[Exams.classId].toString(),
                it[Classes.className],
                it[Classes.sectionName]
            )
        }.map { (classInfo, rows) ->
            ClassExamsDto(
                classId = classInfo.first,
                className = classInfo.second,
                sectionName = classInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                exams = rows.map { mapRowToDto(it) }
            )
        }
    }

    suspend fun getExamsBySubject(academicYearId: String): List<SubjectExamsDto> = tenantDbQuery {
        val exams = Exams
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Subjects.name to SortOrder.ASC, Exams.date to SortOrder.ASC)
            .toList()

        exams.groupBy {
            Triple(
                it[Exams.subjectId].toString(),
                it[Subjects.name],
                it[Subjects.code]
            )
        }.map { (subjectInfo, rows) ->
            SubjectExamsDto(
                subjectId = subjectInfo.first,
                subjectName = subjectInfo.second,
                subjectCode = subjectInfo.third,
                exams = rows.map { mapRowToDto(it) }
            )
        }
    }

    suspend fun getExamsByDate(academicYearId: String): List<ExamsByDateDto> = tenantDbQuery {
        val exams = Exams
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Exams.date to SortOrder.ASC, Exams.name to SortOrder.ASC)
            .toList()

        exams.groupBy { it[Exams.date].toString() }
            .map { (date, rows) ->
                ExamsByDateDto(
                    date = date,
                    exams = rows.map { mapRowToDto(it) }
                )
            }
    }

    private fun mapRowToDto(row: ResultRow): ExamDto {
        return ExamDto(
            id = row[Exams.id].toString(),
            name = row[Exams.name],
            subjectId = row[Exams.subjectId].toString(),
            classId = row[Exams.classId].toString(),
            academicYearId = row[Exams.academicYearId].toString(),
            maxMarks = row[Exams.maxMarks],
            date = row[Exams.date].toString(),
            subjectName = row.getOrNull(Subjects.name),
            subjectCode = row.getOrNull(Subjects.code),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year),
            resultStatus = row[Exams.resultStatus].name,
            resultsPublishedAt = row[Exams.resultsPublishedAt]?.toString()
        )
    }

    suspend fun getExamsByNameGrouped(examName: String?, academicYearId: String): List<ExamByNameDto> = tenantDbQuery {
        val query = Exams
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }

        // If examName is provided, filter by it; otherwise get all exams
        if (!examName.isNullOrBlank()) {
            query.andWhere { Exams.name.lowerCase() like "%${examName.lowercase()}%" }
        }

        val results = query.orderBy(
            Exams.name to SortOrder.ASC,
            Classes.className to SortOrder.ASC,
            Classes.sectionName to SortOrder.ASC,
            Subjects.name to SortOrder.ASC
        ).toList()

        // Group by exam name first
        val examGroups = results.groupBy { it[Exams.name] }

        val examsByName = examGroups.map { (examName, examRows) ->
            // Group by class within each exam
            val classGroups = examRows.groupBy {
                Triple(
                    it[Exams.classId].toString(),
                    it[Classes.className],
                    it[Classes.sectionName]
                )
            }

            val classesWithSubjects = classGroups.map { (classInfo, classRows) ->
                val subjects = classRows.map { row ->
                    SubjectExamDto(
                        examId = row[Exams.id].toString(),
                        subjectId = row[Exams.subjectId].toString(),
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code],
                        maxMarks = row[Exams.maxMarks],
                        date = row[Exams.date].toString(),
                        academicYearId = row[Exams.academicYearId].toString(),
                        academicYearName = row[AcademicYears.year]
                    )
                }

                ClassWithSubjectsExamsDto(
                    classId = classInfo.first,
                    className = classInfo.second,
                    sectionName = classInfo.third,
                    subjects = subjects
                )
            }

            ExamByNameDto(
                examName = examName,
                classes = classesWithSubjects
            )
        }

        examsByName
    }

    suspend fun getExamsName(academicYearId: String): List<String> = tenantDbQuery {
        Exams
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Exams.date)
            .map { it[Exams.name] }
            .distinct()
    }

    suspend fun getExamsClassesName(examName: String, academicYearId: String): List<ClassByExamNameDto> = tenantDbQuery {
        Exams
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .selectAll()
            .where {
                (Exams.name eq examName) and
                        (Exams.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Classes.className to SortOrder.ASC)
            .toList()
            .distinctBy { it[Classes.id] to it[Exams.name] }
            .map {
                ClassByExamNameDto(
                    id = it[Classes.id].toString(),
                    className = it[Classes.className],
                    sectionName = it[Classes.sectionName],
                    academicYearId = it[Classes.academicYearId].toString(),
                    examName = it[Exams.name]
                )
            }
    }

    suspend fun getExamsByClassesAndExamsName(
        classId: String,
        examName: String,
        academicYearId: String
    ): List<ExamDto> = tenantDbQuery {
        Exams
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (Exams.name eq examName) and
                        (Exams.academicYearId eq UUID.fromString(academicYearId)) and
                        (Exams.classId eq UUID.fromString(classId))
            }
            .orderBy(Exams.date to SortOrder.ASC, Exams.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun updateResultStatus(
        examId: String,
        status: ResultStatus
    ) = tenantDbQuery {

        println("Exam ID: $examId, Status: $status")

        Exams.update({ Exams.id eq UUID.fromString(examId) }) {
            it[resultStatus] = status
        }
    }

    suspend fun publishResults(
        examId: String,
        publishedAt: LocalDateTime
    ) = tenantDbQuery {

        Exams.update({ Exams.id eq UUID.fromString(examId) }) {
            it[resultStatus] = ResultStatus.PUBLISHED
            it[resultsPublishedAt] = publishedAt
        }
    }

}