package com.example.repositories

import com.example.database.tables.AcademicYears
import com.example.database.tables.Classes
import com.example.database.tables.StaffClassAssignments
import com.example.database.tables.StaffClassRole
import com.example.database.tables.Users
import com.example.models.dto.*
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class StaffClassAssignmentRepository {

    suspend fun create(request: CreateStaffClassAssignmentRequest): String = dbQuery {
        StaffClassAssignments.insert {
            it[staffId] = UUID.fromString(request.staffId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[role] = request.role?.let { role -> StaffClassRole.valueOf(role) }
        }[StaffClassAssignments.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateStaffClassAssignmentRequest>): List<String> = dbQuery {
        requests.map { request ->
            StaffClassAssignments.insert {
                it[staffId] = UUID.fromString(request.staffId)
                it[classId] = UUID.fromString(request.classId)
                it[academicYearId] = UUID.fromString(request.academicYearId)
                it[role] = request.role?.let { role -> StaffClassRole.valueOf(role) }
            }[StaffClassAssignments.id].toString()
        }
    }

    suspend fun findById(id: String): StaffClassAssignmentDto? = dbQuery {
        StaffClassAssignments
            .join(Users, JoinType.LEFT, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.LEFT, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffClassAssignments.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<StaffClassAssignmentDto> = dbQuery {
        StaffClassAssignments
            .join(Users, JoinType.LEFT, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.LEFT, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Users.firstName to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateStaffClassAssignmentRequest): Boolean = dbQuery {
        StaffClassAssignments.update({ StaffClassAssignments.id eq UUID.fromString(id) }) {
            it[staffId] = UUID.fromString(request.staffId)
            it[classId] = UUID.fromString(request.classId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[role] = request.role?.let { role -> StaffClassRole.valueOf(role) }
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        StaffClassAssignments.deleteWhere { StaffClassAssignments.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByStaffId(staffId: String): List<StaffClassAssignmentDto> = dbQuery {
        StaffClassAssignments
            .join(Users, JoinType.LEFT, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.LEFT, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffClassAssignments.staffId eq UUID.fromString(staffId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassId(classId: String): List<StaffClassAssignmentDto> = dbQuery {
        StaffClassAssignments
            .join(Users, JoinType.LEFT, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.LEFT, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffClassAssignments.classId eq UUID.fromString(classId) }
            .orderBy(Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAcademicYear(academicYearId: String): List<StaffClassAssignmentDto> = dbQuery {
        StaffClassAssignments
            .join(Users, JoinType.LEFT, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.LEFT, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffClassAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Users.firstName to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStaffAndAcademicYear(staffId: String, academicYearId: String): List<StaffClassAssignmentDto> = dbQuery {
        StaffClassAssignments
            .join(Users, JoinType.LEFT, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.LEFT, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (StaffClassAssignments.staffId eq UUID.fromString(staffId)) and
                        (StaffClassAssignments.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByRole(role: String): List<StaffClassAssignmentDto> = dbQuery {
        StaffClassAssignments
            .join(Users, JoinType.LEFT, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.LEFT, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.LEFT, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffClassAssignments.role eq StaffClassRole.valueOf(role) }
            .orderBy(Users.firstName to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(staffId: String, classId: String, academicYearId: String, excludeId: String? = null): Boolean = dbQuery {
        val query = StaffClassAssignments.selectAll()
            .where {
                (StaffClassAssignments.staffId eq UUID.fromString(staffId)) and
                        (StaffClassAssignments.classId eq UUID.fromString(classId)) and
                        (StaffClassAssignments.academicYearId eq UUID.fromString(academicYearId))
            }

        if (excludeId != null) {
            query.andWhere { StaffClassAssignments.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByStaffId(staffId: String): Int = dbQuery {
        StaffClassAssignments.deleteWhere { StaffClassAssignments.staffId eq UUID.fromString(staffId) }
    }

    suspend fun deleteByClassId(classId: String): Int = dbQuery {
        StaffClassAssignments.deleteWhere { StaffClassAssignments.classId eq UUID.fromString(classId) }
    }

    suspend fun deleteByAcademicYear(academicYearId: String): Int = dbQuery {
        StaffClassAssignments.deleteWhere { StaffClassAssignments.academicYearId eq UUID.fromString(academicYearId) }
    }

    suspend fun getStaffWithClasses(academicYearId: String): List<StaffWithClassesDto> = dbQuery {
        val staffAssignments = StaffClassAssignments
            .join(Users, JoinType.INNER, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.INNER, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffClassAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Users.firstName to SortOrder.ASC, Classes.className to SortOrder.ASC)
            .toList()

        staffAssignments.groupBy {
            Triple(
                it[StaffClassAssignments.staffId].toString(),
                it[Users.firstName],
                it[Users.email]
            )
        }.map { (staffInfo, rows) ->
            StaffWithClassesDto(
                staffId = staffInfo.first,
                staffName = staffInfo.second,
                staffEmail = staffInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                classes = rows.map { row ->
                    ClassWithRoleDto(
                        id = row[StaffClassAssignments.classId].toString(),
                        className = row[Classes.className],
                        sectionName = row[Classes.sectionName],
                        role = row[StaffClassAssignments.role]?.name
                    )
                }
            )
        }
    }

    suspend fun getClassesWithStaff(academicYearId: String): List<ClassWithStaffDto> = dbQuery {
        val staffAssignments = StaffClassAssignments
            .join(Users, JoinType.INNER, StaffClassAssignments.staffId, Users.id)
            .join(Classes, JoinType.INNER, StaffClassAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StaffClassAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StaffClassAssignments.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .toList()

        staffAssignments.groupBy {
            Triple(
                it[StaffClassAssignments.classId].toString(),
                it[Classes.className],
                it[Classes.sectionName]
            )
        }.map { (classInfo, rows) ->
            ClassWithStaffDto(
                classId = classInfo.first,
                className = classInfo.second,
                sectionName = classInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                staff = rows.map { row ->
                    StaffWithRoleDto(
                        id = row[StaffClassAssignments.staffId].toString(),
                        name = row[Users.firstName],
                        email = row[Users.email],
                        role = row[StaffClassAssignments.role]?.name
                    )
                }
            )
        }
    }

    private fun mapRowToDto(row: ResultRow): StaffClassAssignmentDto {
        return StaffClassAssignmentDto(
            id = row[StaffClassAssignments.id].toString(),
            staffId = row[StaffClassAssignments.staffId].toString(),
            classId = row[StaffClassAssignments.classId].toString(),
            academicYearId = row[StaffClassAssignments.academicYearId].toString(),
            role = row[StaffClassAssignments.role]?.name,
            staffName = row.getOrNull(Users.firstName),
            staffEmail = row.getOrNull(Users.email),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            academicYearName = row.getOrNull(AcademicYears.year)
        )
    }
}