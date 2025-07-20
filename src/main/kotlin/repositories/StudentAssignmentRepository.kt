package com.example.repositories

import com.example.database.tables.AcademicYears
import com.example.database.tables.Classes
import com.example.database.tables.StudentAssignments
import com.example.database.tables.UserRole
import com.example.database.tables.Users
import com.example.models.dto.*
import com.example.utils.dbQuery
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class StudentAssignmentRepository {

    suspend fun create(request: CreateStudentAssignmentRequest): String = tenantDbQuery {
        StudentAssignments.insert {
            it[studentId] = UUID.fromString(request.studentId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
        }[StudentAssignments.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateStudentAssignmentRequest>): List<String> = tenantDbQuery {
        requests.map { request ->
            StudentAssignments.insert {
                it[studentId] = UUID.fromString(request.studentId)
                it[classId] = UUID.fromString(request.classId)
                it[academicYearId] = UUID.fromString(request.academicYearId)
            }[StudentAssignments.id].toString()
        }
    }

    suspend fun findById(id: String): StudentAssignmentDto? = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<StudentAssignmentDto> = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateStudentAssignmentRequest): Boolean = tenantDbQuery {
        StudentAssignments.update({ StudentAssignments.id eq UUID.fromString(id) }) {
            it[studentId] = UUID.fromString(request.studentId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
        } > 0
    }

    suspend fun delete(id: String): Boolean = tenantDbQuery {
        StudentAssignments.deleteWhere { StudentAssignments.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByStudentId(studentId: String): List<StudentAssignmentDto> = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.studentId eq UUID.fromString(studentId) }
            .orderBy(AcademicYears.year to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassId(classId: String): List<StudentAssignmentDto> = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.classId eq UUID.fromString(classId) }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAcademicYear(academicYearId: String): List<StudentAssignmentDto> = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndAcademicYear(classId: String, academicYearId: String): List<StudentAssignmentDto> = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (StudentAssignments.classId eq UUID.fromString(classId)) and
                        (StudentAssignments.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStudentAndAcademicYear(studentId: String, academicYearId: String): StudentAssignmentDto? = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (StudentAssignments.studentId eq UUID.fromString(studentId)) and
                        (StudentAssignments.academicYearId eq UUID.fromString(academicYearId))
            }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun checkDuplicate(studentId: String, classId: String, academicYearId: String, excludeId: String? = null): Boolean = tenantDbQuery {
        val query = StudentAssignments.selectAll()
            .where {
                (StudentAssignments.studentId eq UUID.fromString(studentId)) and
                        (StudentAssignments.classId eq UUID.fromString(classId)) and
                        (StudentAssignments.academicYearId eq UUID.fromString(academicYearId))
            }

        if (excludeId != null) {
            query.andWhere { StudentAssignments.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByStudentId(studentId: String): Int = tenantDbQuery {
        StudentAssignments.deleteWhere { StudentAssignments.studentId eq UUID.fromString(studentId) }
    }

    suspend fun deleteByStudentAndAcademicYear(studentId: String, academicYearId: String): Int = tenantDbQuery {
        StudentAssignments.deleteWhere { (StudentAssignments.studentId eq UUID.fromString(studentId)) and (StudentAssignments.academicYearId eq UUID.fromString(academicYearId)) }
    }

    suspend fun deleteByClassId(classId: String): Int = tenantDbQuery {
        StudentAssignments.deleteWhere { StudentAssignments.classId eq UUID.fromString(classId) }
    }

    suspend fun deleteByClassAndAcademicYear(classId: String, academicYearId: String): Int = tenantDbQuery {
        StudentAssignments.deleteWhere { (StudentAssignments.classId eq UUID.fromString(classId)) and (StudentAssignments.academicYearId eq UUID.fromString(academicYearId)) }
    }

    suspend fun deleteByAcademicYear(academicYearId: String): Int = tenantDbQuery {
        StudentAssignments.deleteWhere { StudentAssignments.academicYearId eq UUID.fromString(academicYearId) }
    }

    suspend fun transferStudents(studentIds: List<String>, fromClassId: String, toClassId: String, academicYearId: String): Int = tenantDbQuery {
        var updatedCount = 0
        studentIds.forEach { studentId ->
            val updated = StudentAssignments.update({
                (StudentAssignments.studentId eq UUID.fromString(studentId)) and
                        (StudentAssignments.classId eq UUID.fromString(fromClassId)) and
                        (StudentAssignments.academicYearId eq UUID.fromString(academicYearId))
            }) {
                it[classId] = UUID.fromString(toClassId)
            }
            updatedCount += updated
        }
        updatedCount
    }

    suspend fun getClassesWithStudents(academicYearId: String): List<ClassWithStudentsDto> = tenantDbQuery {
        val studentAssignments = StudentAssignments
            .join(Users, JoinType.INNER, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.INNER, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .toList()

        studentAssignments.groupBy {
            Triple(
                it[StudentAssignments.classId].toString(),
                it[Classes.className],
                it[Classes.sectionName]
            )
        }.map { (classInfo, rows) ->
            ClassWithStudentsDto(
                classId = classInfo.first,
                className = classInfo.second,
                sectionName = classInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                students = rows.map { row ->
                    StudentDto(
                        id = row[StudentAssignments.studentId].toString(),
                        firstName = row[Users.firstName],
                        lastName = row[Users.lastName],
                        email = row[Users.email],
                        mobileNumber = row[Users.mobileNumber]
                    )
                }
            )
        }
    }

    suspend fun getStudentsWithClasses(academicYearId: String): List<StudentWithClassDto> = tenantDbQuery {
        val studentAssignments = StudentAssignments
            .join(Users, JoinType.INNER, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.INNER, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Users.firstName to SortOrder.ASC)
            .toList()

        studentAssignments.groupBy {
            Triple(
                it[StudentAssignments.studentId].toString(),
                it[Users.firstName],
                it[Users.email]
            )
        }.map { (studentInfo, rows) ->
            StudentWithClassDto(
                studentId = studentInfo.first,
                studentName = studentInfo.second,
                studentEmail = studentInfo.third,
                studentPhone = rows.first()[Users.mobileNumber],
                classAssignments = rows.map { row ->
                    ClassAssignmentDto(
                        assignmentId = row[StudentAssignments.id].toString(),
                        classId = row[StudentAssignments.classId].toString(),
                        className = row[Classes.className],
                        sectionName = row[Classes.sectionName],
                        academicYearId = row[StudentAssignments.academicYearId].toString(),
                        academicYearName = row[AcademicYears.year] ?: ""
                    )
                }
            )
        }
    }

    suspend fun getStudentCountByClass(academicYearId: String): List<Pair<String, Int>> = tenantDbQuery {
        StudentAssignments
            .join(Classes, JoinType.INNER, StudentAssignments.classId, Classes.id)
            .select(Classes.className, Classes.sectionName, StudentAssignments.classId.count())
            .where { StudentAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .groupBy(StudentAssignments.classId, Classes.className, Classes.sectionName)
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { row ->
                val className = row[Classes.className]
                val sectionName = row[Classes.sectionName]
                val count = row[StudentAssignments.classId.count()].toInt()
                "$className - $sectionName" to count
            }
    }

    suspend fun findStudentsNotInClass(classId: String, academicYearId: String): List<StudentDto> = tenantDbQuery {
        val assignedStudentIds = StudentAssignments
            .select(StudentAssignments.studentId)
            .where {
                (StudentAssignments.classId eq UUID.fromString(classId)) and
                        (StudentAssignments.academicYearId eq UUID.fromString(academicYearId))
            }
            .map { it[StudentAssignments.studentId] }

        Users
            .selectAll()
            .where { Users.id notInList assignedStudentIds }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { row ->
                StudentDto(
                    id = row[Users.id].toString(),
                    firstName = row[Users.firstName],
                    lastName = row[Users.lastName],
                    email = row[Users.email],
                    mobileNumber = row[Users.mobileNumber]
                )
            }
    }

    suspend fun findUnassignedStudents(academicYearId: String): List<StudentDto> = tenantDbQuery {
        val assignedStudentIds = StudentAssignments
            .select(StudentAssignments.studentId)
            .where { StudentAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .map { it[StudentAssignments.studentId] }

        Users
            .selectAll()
            .where {
                (Users.id notInList assignedStudentIds) and
                        (Users.role eq UserRole.STUDENT)
            }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { row ->
                StudentDto(
                    id = row[Users.id].toString(),
                    firstName = row[Users.firstName],
                    lastName = row[Users.lastName],
                    email = row[Users.email],
                    mobileNumber = row[Users.mobileNumber]
                )
            }
    }

    suspend fun getAssignmentHistory(studentId: String): List<StudentAssignmentDto> = tenantDbQuery {
        StudentAssignments
            .join(Users, JoinType.LEFT, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.LEFT, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.studentId eq UUID.fromString(studentId) }
            .orderBy(AcademicYears.year to SortOrder.DESC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    private fun mapRowToDto(row: ResultRow): StudentAssignmentDto {
        return StudentAssignmentDto(
            id = row[StudentAssignments.id].toString(),
            studentId = row[StudentAssignments.studentId].toString(),
            classId = row[StudentAssignments.classId].toString(),
            academicYearId = row[StudentAssignments.academicYearId].toString(),
            firstName = row.getOrNull(Users.firstName),
            lastName = row.getOrNull(Users.lastName),
            email = row.getOrNull(Users.email),
            mobileNumber = row.getOrNull(Users.mobileNumber),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year)
        )
    }
}