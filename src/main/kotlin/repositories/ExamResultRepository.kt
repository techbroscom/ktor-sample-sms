// Fixed Repository with correct table relationships
package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ExamResultRepository {

    suspend fun create(request: CreateExamResultRequest): String = dbQuery {
        ExamResults.insert {
            it[examId] = UUID.fromString(request.examId)
            it[studentId] = UUID.fromString(request.studentId)
            it[marksObtained] = request.marksObtained
            it[grade] = request.grade
        }[ExamResults.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateExamResultRequest>): List<String> = dbQuery {
        requests.map { request ->
            ExamResults.insert {
                it[examId] = UUID.fromString(request.examId)
                it[studentId] = UUID.fromString(request.studentId)
                it[marksObtained] = request.marksObtained
                it[grade] = request.grade
            }[ExamResults.id].toString()
        }
    }

    suspend fun findById(id: String): ExamResultDto? = dbQuery {
        ExamResults
            .join(Exams, JoinType.LEFT, ExamResults.examId, Exams.id)
            .join(Users, JoinType.LEFT, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .join(StudentAssignments, JoinType.LEFT, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .where { ExamResults.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<ExamResultDto> = dbQuery {
        ExamResults
            .join(Exams, JoinType.LEFT, ExamResults.examId, Exams.id)
            .join(Users, JoinType.LEFT, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .join(StudentAssignments, JoinType.LEFT, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .orderBy(Exams.name to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateExamResultRequest): Boolean = dbQuery {
        ExamResults.update({ ExamResults.id eq UUID.fromString(id) }) {
            it[examId] = UUID.fromString(request.examId)
            it[studentId] = UUID.fromString(request.studentId)
            it[marksObtained] = request.marksObtained
            it[grade] = request.grade
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        ExamResults.deleteWhere { ExamResults.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByExamId(examId: String): List<ExamResultDto> = dbQuery {
        ExamResults
            .join(Exams, JoinType.LEFT, ExamResults.examId, Exams.id)
            .join(Users, JoinType.LEFT, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .join(StudentAssignments, JoinType.LEFT, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .where { ExamResults.examId eq UUID.fromString(examId) }
            .orderBy(Users.firstName to SortOrder.ASC, Users.lastName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStudentId(studentId: String): List<ExamResultDto> = dbQuery {
        ExamResults
            .join(Exams, JoinType.LEFT, ExamResults.examId, Exams.id)
            .join(Users, JoinType.LEFT, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .join(StudentAssignments, JoinType.LEFT, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .where { ExamResults.studentId eq UUID.fromString(studentId) }
            .orderBy(Exams.date to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndExam(classId: String, examId: String): List<ExamResultDto> = dbQuery {
        ExamResults
            .join(Exams, JoinType.INNER, ExamResults.examId, Exams.id)
            .join(Users, JoinType.INNER, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.LEFT, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, Exams.academicYearId, AcademicYears.id)
            .join(StudentAssignments, JoinType.INNER, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .where {
                (Exams.classId eq UUID.fromString(classId)) and
                        (ExamResults.examId eq UUID.fromString(examId))
            }
            .orderBy(Users.firstName to SortOrder.ASC, Users.lastName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(examId: String, studentId: String, excludeId: String? = null): Boolean = dbQuery {
        val query = ExamResults.selectAll()
            .where {
                (ExamResults.examId eq UUID.fromString(examId)) and
                        (ExamResults.studentId eq UUID.fromString(studentId))
            }

        if (excludeId != null) {
            query.andWhere { ExamResults.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByExamId(examId: String): Int = dbQuery {
        ExamResults.deleteWhere { ExamResults.examId eq UUID.fromString(examId) }
    }

    suspend fun deleteByStudentId(studentId: String): Int = dbQuery {
        ExamResults.deleteWhere { ExamResults.studentId eq UUID.fromString(studentId) }
    }

    suspend fun getExamsWithResults(academicYearId: String): List<ExamWithResultsDto> = dbQuery {
        val examResults = ExamResults
            .join(Exams, JoinType.INNER, ExamResults.examId, Exams.id)
            .join(Users, JoinType.INNER, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .join(StudentAssignments, JoinType.INNER, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Exams.name to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .toList()

        examResults.groupBy {
            ExamInfo(
                it[ExamResults.examId].toString(),
                it[Exams.name],
                it[Subjects.name],
                it[Subjects.code],
                it[Exams.maxMarks],
                it[Exams.date].toString(),
                it[AcademicYears.year]
            )
        }.map { (examInfo, rows) ->
            ExamWithResultsDto(
                examId = examInfo.id,
                examName = examInfo.name,
                subjectName = examInfo.subjectName,
                subjectCode = examInfo.subjectCode,
                maxMarks = examInfo.maxMarks,
                examDate = examInfo.date,
                academicYearName = examInfo.academicYearName,
                results = rows.map { row ->
                    StudentResultDto(
                        id = row[ExamResults.id].toString(),
                        studentId = row[ExamResults.studentId].toString(),
                        studentName = "${row[Users.firstName]} ${row[Users.lastName]}",
                        studentEmail = row[Users.email],
                        rollNumber = "", // Will need to get from StudentAssignments if you add rollNumber field
                        className = row[Classes.className],
                        sectionName = row[Classes.sectionName],
                        marksObtained = row[ExamResults.marksObtained],
                        grade = row[ExamResults.grade]
                    )
                }
            )
        }
    }

    suspend fun getStudentsWithExamResults(academicYearId: String): List<StudentWithExamResultsDto> = dbQuery {
        val examResults = ExamResults
            .join(Exams, JoinType.INNER, ExamResults.examId, Exams.id)
            .join(Users, JoinType.INNER, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .join(StudentAssignments, JoinType.INNER, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .where { Exams.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Users.firstName to SortOrder.ASC, Exams.date to SortOrder.DESC)
            .toList()

        examResults.groupBy {
            StudentInfo(
                it[ExamResults.studentId].toString(),
                "${it[Users.firstName]} ${it[Users.lastName]}",
                it[Users.email],
                "", // Will need to get rollNumber from StudentAssignments if you add that field
                it[Classes.className],
                it[Classes.sectionName],
                it[AcademicYears.year]
            )
        }.map { (studentInfo, rows) ->
            StudentWithExamResultsDto(
                studentId = studentInfo.id,
                studentName = studentInfo.name,
                studentEmail = studentInfo.email,
                rollNumber = studentInfo.rollNumber,
                className = studentInfo.className,
                sectionName = studentInfo.sectionName,
                academicYearName = studentInfo.academicYearName,
                results = rows.map { row ->
                    ExamResultDetailDto(
                        id = row[ExamResults.id].toString(),
                        examId = row[ExamResults.examId].toString(),
                        examName = row[Exams.name],
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code],
                        maxMarks = row[Exams.maxMarks],
                        examDate = row[Exams.date].toString(),
                        marksObtained = row[ExamResults.marksObtained],
                        grade = row[ExamResults.grade]
                    )
                }
            )
        }
    }

    suspend fun getClassResultSummary(classId: String, examId: String): ClassResultSummaryDto? = dbQuery {
        val results = ExamResults
            .join(Exams, JoinType.INNER, ExamResults.examId, Exams.id)
            .join(Users, JoinType.INNER, ExamResults.studentId, Users.id)
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(StudentAssignments, JoinType.INNER, additionalConstraint = {
                (StudentAssignments.studentId eq ExamResults.studentId) and
                        (StudentAssignments.classId eq Exams.classId) and
                        (StudentAssignments.academicYearId eq Exams.academicYearId)
            })
            .selectAll()
            .where {
                (Exams.classId eq UUID.fromString(classId)) and
                        (ExamResults.examId eq UUID.fromString(examId))
            }
            .toList()

        if (results.isEmpty()) return@dbQuery null

        val firstResult = results.first()
        val marks = results.map { it[ExamResults.marksObtained] }
        val maxPossibleMarks = firstResult[Exams.maxMarks]
        val passMarks = maxPossibleMarks * 0.4 // Assuming 40% is pass
        val passedStudents = marks.count { it >= passMarks }

        // Get total students in class for this academic year
        val totalStudents = StudentAssignments
            .selectAll()
            .where {
                (StudentAssignments.classId eq UUID.fromString(classId)) and
                        (StudentAssignments.academicYearId eq firstResult[Exams.academicYearId])
            }
            .count().toInt()

        ClassResultSummaryDto(
            classId = classId,
            className = firstResult[Classes.className],
            sectionName = firstResult[Classes.sectionName],
            examId = examId,
            examName = firstResult[Exams.name],
            subjectName = firstResult[Subjects.name],
            totalStudents = totalStudents,
            studentsAppeared = marks.size,
            averageMarks = marks.average(),
            highestMarks = marks.maxOrNull() ?: 0,
            lowestMarks = marks.minOrNull() ?: 0,
            passPercentage = if (marks.isNotEmpty()) (passedStudents.toDouble() / marks.size) * 100 else 0.0
        )
    }

    private fun mapRowToDto(row: ResultRow): ExamResultDto {
        return ExamResultDto(
            id = row[ExamResults.id].toString(),
            examId = row[ExamResults.examId].toString(),
            studentId = row[ExamResults.studentId].toString(),
            marksObtained = row[ExamResults.marksObtained],
            grade = row[ExamResults.grade],
            examName = row.getOrNull(Exams.name),
            subjectName = row.getOrNull(Subjects.name),
            subjectCode = row.getOrNull(Subjects.code),
            maxMarks = row.getOrNull(Exams.maxMarks),
            examDate = row.getOrNull(Exams.date)?.toString(),
            studentName = row.getOrNull(Users.firstName)?.let { firstName ->
                val lastName = row.getOrNull(Users.lastName) ?: ""
                "$firstName $lastName".trim()
            },
            studentEmail = row.getOrNull(Users.email),
            rollNumber = "", // You may want to add rollNumber to StudentAssignments table
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
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

    private data class StudentInfo(
        val id: String,
        val name: String,
        val email: String,
        val rollNumber: String,
        val className: String,
        val sectionName: String,
        val academicYearName: String
    )
}