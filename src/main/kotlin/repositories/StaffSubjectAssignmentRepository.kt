package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class StaffSubjectAssignmentRepository {

    suspend fun create(request: CreateStaffSubjectAssignmentRequest): String = tenantDbQuery {
        StaffSubjectAssignments.insert {
            it[staffId] = UUID.fromString(request.staffId)
            it[classSubjectId] = UUID.fromString(request.classSubjectId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
        }[StaffSubjectAssignments.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateStaffSubjectAssignmentRequest>): List<String> = tenantDbQuery {
        requests.map { request ->
            StaffSubjectAssignments.insert {
                it[staffId] = UUID.fromString(request.staffId)
                it[classSubjectId] = UUID.fromString(request.classSubjectId)
                it[classId] = UUID.fromString(request.classId)
                it[academicYearId] = UUID.fromString(request.academicYearId)
            }[StaffSubjectAssignments.id].toString()
        }
    }

    suspend fun findById(id: String): StaffSubjectAssignmentDto? = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffSubjectAssignments.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Users.firstName to SortOrder.ASC, Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateStaffSubjectAssignmentRequest): Boolean = tenantDbQuery {
        StaffSubjectAssignments.update({ StaffSubjectAssignments.id eq UUID.fromString(id) }) {
            it[staffId] = UUID.fromString(request.staffId)
            it[classSubjectId] = UUID.fromString(request.classSubjectId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
        } > 0
    }

    suspend fun delete(id: String): Boolean = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { StaffSubjectAssignments.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByStaffId(staffId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffSubjectAssignments.staffId eq UUID.fromString(staffId) }
            .orderBy(Subjects.name to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassId(classId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffSubjectAssignments.classId eq UUID.fromString(classId) }
            .orderBy(Subjects.name to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassSubjectId(classSubjectId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffSubjectAssignments.classSubjectId eq UUID.fromString(classSubjectId) }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassSubjectAndAcademicYear(classSubjectId: String, academicYearId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { (StaffSubjectAssignments.classSubjectId eq UUID.fromString(classSubjectId)) and (StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId)) }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAcademicYear(academicYearId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Users.firstName to SortOrder.ASC, Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStaffAndAcademicYear(staffId: String, academicYearId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (StaffSubjectAssignments.staffId eq UUID.fromString(staffId)) and
                        (StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Subjects.name to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndAcademicYear(classId: String, academicYearId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        StaffSubjectAssignments
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.LEFT, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.LEFT, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (StaffSubjectAssignments.classId eq UUID.fromString(classId)) and
                        (StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Subjects.name to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(staffId: String, classSubjectId: String, classId: String, academicYearId: String, excludeId: String? = null): Boolean = tenantDbQuery {
        val query = StaffSubjectAssignments.selectAll()
            .where {
                (StaffSubjectAssignments.staffId eq UUID.fromString(staffId)) and
                        (StaffSubjectAssignments.classSubjectId eq UUID.fromString(classSubjectId)) and
                        (StaffSubjectAssignments.classId eq UUID.fromString(classId)) and
                        (StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId))
            }

        if (excludeId != null) {
            query.andWhere { StaffSubjectAssignments.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByStaffId(staffId: String): Int = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { StaffSubjectAssignments.staffId eq UUID.fromString(staffId) }
    }

    suspend fun deleteByStaffAndAcademicYear(staffId: String, academicYearId: String): Int = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { (StaffSubjectAssignments.staffId eq UUID.fromString(staffId)) and (StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId))}
    }

    suspend fun deleteByClassId(classId: String): Int = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { StaffSubjectAssignments.classId eq UUID.fromString(classId) }
    }

    suspend fun deleteByClassAndAcademicYear(classId: String, academicYearId: String): Int = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { (StaffSubjectAssignments.classId eq UUID.fromString(classId)) and (StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId)) }
    }

    suspend fun deleteByClassSubjectId(classSubjectId: String): Int = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { StaffSubjectAssignments.classSubjectId eq UUID.fromString(classSubjectId) }
    }

    suspend fun deleteByClassSubjectAndAcademicYear(classSubjectId: String, academicYearId: String): Int = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { (StaffSubjectAssignments.classSubjectId eq UUID.fromString(classSubjectId)) and (StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId)) }
    }

    suspend fun deleteByAcademicYear(academicYearId: String): Int = tenantDbQuery {
        StaffSubjectAssignments.deleteWhere { StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId) }
    }

    suspend fun getStaffWithSubjects(academicYearId: String): List<StaffWithSubjectsDto> = tenantDbQuery {
        val staffAssignments = StaffSubjectAssignments
            .join(Users, JoinType.INNER, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.INNER, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Users.firstName to SortOrder.ASC, Subjects.name to SortOrder.ASC)
            .toList()

        staffAssignments.groupBy {
            Triple(
                it[StaffSubjectAssignments.staffId].toString(),
                it[Users.firstName],
                it[Users.email]
            )
        }.map { (staffInfo, rows) ->
            StaffWithSubjectsDto(
                staffId = staffInfo.first,
                staffName = staffInfo.second,
                staffEmail = staffInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                subjects = rows.map { row ->
                    SubjectWithClassDto(
                        id = row[StaffSubjectAssignments.id].toString(),
                        classSubjectId = row[StaffSubjectAssignments.classSubjectId].toString(),
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code],
                        className = row[Classes.className],
                        sectionName = row[Classes.sectionName]
                    )
                }
            )
        }
    }

    suspend fun getClassesWithSubjectStaff(academicYearId: String): List<ClassWithSubjectStaffDto> = tenantDbQuery {
        val staffAssignments = StaffSubjectAssignments
            .join(Users, JoinType.INNER, StaffSubjectAssignments.staffId, Users.id)
            .join(ClassSubjects, JoinType.INNER, StaffSubjectAssignments.classSubjectId, ClassSubjects.id)
            .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, StaffSubjectAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffSubjectAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .toList()

        staffAssignments.groupBy {
            Triple(
                it[StaffSubjectAssignments.classId].toString(),
                it[Classes.className],
                it[Classes.sectionName]
            )
        }.map { (classInfo, rows) ->
            ClassWithSubjectStaffDto(
                classId = classInfo.first,
                className = classInfo.second,
                sectionName = classInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                subjects = rows.groupBy {
                    Triple(
                        it[StaffSubjectAssignments.classSubjectId].toString(),
                        it[Subjects.name],
                        it[Subjects.code]
                    )
                }.map { (subjectInfo, subjectRows) ->
                    SubjectWithStaffDto(
                        id = subjectRows.first()[StaffSubjectAssignments.id].toString(),
                        classSubjectId = subjectInfo.first,
                        subjectName = subjectInfo.second,
                        subjectCode = subjectInfo.third,
                        staff = subjectRows.map { row ->
                            StaffBasicDto(
                                id = row[StaffSubjectAssignments.staffId].toString(),
                                name = row[Users.firstName],
                                email = row[Users.email]
                            )
                        }
                    )
                }
            )
        }
    }

    private fun mapRowToDto(row: ResultRow): StaffSubjectAssignmentDto {
        return StaffSubjectAssignmentDto(
            id = row[StaffSubjectAssignments.id].toString(),
            staffId = row[StaffSubjectAssignments.staffId].toString(),
            classSubjectId = row[StaffSubjectAssignments.classSubjectId].toString(),
            classId = row[StaffSubjectAssignments.classId].toString(),
            academicYearId = row[StaffSubjectAssignments.academicYearId].toString(),
            staffName = row.getOrNull(Users.firstName),
            staffEmail = row.getOrNull(Users.email),
            subjectId = row.getOrNull(Subjects.id).toString(),
            subjectName = row.getOrNull(Subjects.name),
            subjectCode = row.getOrNull(Subjects.code),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year)
        )
    }

    suspend fun findByClassIdWithAllSubjects(classId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        ClassSubjects
            .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, ClassSubjects.classId, Classes.id)
            .join(StaffSubjectAssignments, JoinType.LEFT, ClassSubjects.id, StaffSubjectAssignments.classSubjectId)
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.classId eq UUID.fromString(classId) }
            .orderBy(Subjects.name to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDtoWithNullableStaff(it) }
    }

    suspend fun findByClassIdWithAllSubjectsForActiveYear(classId: String, activeAcademicYearId: String): List<StaffSubjectAssignmentDto> = tenantDbQuery {
        ClassSubjects
            .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, ClassSubjects.classId, Classes.id)
            .join(StaffSubjectAssignments, JoinType.LEFT,
                additionalConstraint = {
                    (ClassSubjects.id eq StaffSubjectAssignments.classSubjectId) and
                            (StaffSubjectAssignments.academicYearId eq UUID.fromString(activeAcademicYearId))
                }
            )
            .join(Users, JoinType.LEFT, StaffSubjectAssignments.staffId, Users.id)
            .join(AcademicYears, JoinType.LEFT, StaffSubjectAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.classId eq UUID.fromString(classId) }
            .orderBy(Subjects.name to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDtoWithNullableStaff(it, activeAcademicYearId) }
    }

    // Modified mapping function to handle null staff assignments
    private fun mapRowToDtoWithNullableStaff(row: ResultRow, academicYearId: String? = null): StaffSubjectAssignmentDto {
        return StaffSubjectAssignmentDto(
            id = row.getOrNull(StaffSubjectAssignments.id)?.toString() ?: "", // Empty if no assignment
            staffId = row.getOrNull(StaffSubjectAssignments.staffId)?.toString() ?: "", // Empty if no staff assigned
            classSubjectId = row[ClassSubjects.id].toString(),
            classId = row[Classes.id].toString(),
            academicYearId = academicYearId ?: row.getOrNull(StaffSubjectAssignments.academicYearId)?.toString() ?: "",
            staffName = row.getOrNull(Users.firstName),
            staffEmail = row.getOrNull(Users.email),
            subjectName = row.getOrNull(Subjects.name),
            subjectCode = row.getOrNull(Subjects.code),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year)
        )
    }
}