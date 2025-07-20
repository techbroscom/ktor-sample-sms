package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.CreateStudentTransportAssignmentRequest
import com.example.models.dto.StudentTransportAssignmentDto
import com.example.models.dto.UpdateStudentTransportAssignmentRequest
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.util.*

class StudentTransportAssignmentRepository {

    suspend fun create(request: CreateStudentTransportAssignmentRequest): UUID = tenantDbQuery {
        val assignmentId = UUID.randomUUID()
        StudentTransportAssignments.insert {
            it[id] = assignmentId
            it[studentId] = UUID.fromString(request.studentId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[routeId] = UUID.fromString(request.routeId)
            it[stopId] = UUID.fromString(request.stopId)
            it[startDate] = LocalDate.parse(request.startDate)
            it[endDate] = request.endDate?.let { LocalDate.parse(it) }
            it[isActive] = request.isActive
        }
        assignmentId
    }

    suspend fun bulkCreate(requests: List<CreateStudentTransportAssignmentRequest>): List<UUID> = tenantDbQuery {
        val assignmentIds = mutableListOf<UUID>()
        StudentTransportAssignments.batchInsert(requests) { request ->
            val assignmentId = UUID.randomUUID()
            assignmentIds.add(assignmentId)
            this[StudentTransportAssignments.id] = assignmentId
            this[StudentTransportAssignments.studentId] = UUID.fromString(request.studentId)
            this[StudentTransportAssignments.academicYearId] = UUID.fromString(request.academicYearId)
            this[StudentTransportAssignments.routeId] = UUID.fromString(request.routeId)
            this[StudentTransportAssignments.stopId] = UUID.fromString(request.stopId)
            this[StudentTransportAssignments.startDate] = LocalDate.parse(request.startDate)
            this[StudentTransportAssignments.endDate] = request.endDate?.let { LocalDate.parse(it) }
            this[StudentTransportAssignments.isActive] = request.isActive
        }
        assignmentIds
    }

    suspend fun findById(id: UUID): StudentTransportAssignmentDto? = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .where { StudentTransportAssignments.id eq id }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<StudentTransportAssignmentDto> = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .orderBy(Users.firstName to SortOrder.ASC, Users.lastName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStudentId(studentId: UUID): List<StudentTransportAssignmentDto> = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .where { StudentTransportAssignments.studentId eq studentId }
            .orderBy(StudentTransportAssignments.startDate to SortOrder.DESC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAcademicYearId(academicYearId: UUID): List<StudentTransportAssignmentDto> = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .where { StudentTransportAssignments.academicYearId eq academicYearId }
            .orderBy(TransportRoutes.name to SortOrder.ASC, TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByRouteId(routeId: UUID): List<StudentTransportAssignmentDto> = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .where { StudentTransportAssignments.routeId eq routeId }
            .orderBy(TransportStops.orderIndex to SortOrder.ASC, Users.firstName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByStopId(stopId: UUID): List<StudentTransportAssignmentDto> = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .where { StudentTransportAssignments.stopId eq stopId }
            .orderBy(Users.firstName to SortOrder.ASC, Users.lastName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findActive(): List<StudentTransportAssignmentDto> = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .where { StudentTransportAssignments.isActive eq true }
            .orderBy(TransportRoutes.name to SortOrder.ASC, TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findActiveByAcademicYear(academicYearId: UUID): List<StudentTransportAssignmentDto> = tenantDbQuery {
        StudentTransportAssignments
            .join(Users, JoinType.INNER, StudentTransportAssignments.studentId, Users.id)
            .join(AcademicYears, JoinType.INNER, StudentTransportAssignments.academicYearId, AcademicYears.id)
            .join(TransportRoutes, JoinType.INNER, StudentTransportAssignments.routeId, TransportRoutes.id)
            .join(TransportStops, JoinType.INNER, StudentTransportAssignments.stopId, TransportStops.id)
            .selectAll()
            .where {
                (StudentTransportAssignments.academicYearId eq academicYearId) and
                        (StudentTransportAssignments.isActive eq true)
            }
            .orderBy(TransportRoutes.name to SortOrder.ASC, TransportStops.orderIndex to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: UUID, request: UpdateStudentTransportAssignmentRequest): Boolean = tenantDbQuery {
        StudentTransportAssignments.update({ StudentTransportAssignments.id eq id }) {
            it[studentId] = UUID.fromString(request.studentId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
            it[routeId] = UUID.fromString(request.routeId)
            it[stopId] = UUID.fromString(request.stopId)
            it[startDate] = LocalDate.parse(request.startDate)
            it[endDate] = request.endDate?.let { LocalDate.parse(it) }
            it[isActive] = request.isActive
        } > 0
    }

    suspend fun delete(id: UUID): Boolean = tenantDbQuery {
        StudentTransportAssignments.deleteWhere { StudentTransportAssignments.id eq id } > 0
    }

    suspend fun assignmentExistsForStudentInAcademicYear(studentId: UUID, academicYearId: UUID): Boolean = tenantDbQuery {
        StudentTransportAssignments.selectAll()
            .where {
                (StudentTransportAssignments.studentId eq studentId) and
                        (StudentTransportAssignments.academicYearId eq academicYearId)
            }
            .count() > 0
    }

    suspend fun assignmentExistsForStudentInAcademicYearExcludingId(
        studentId: UUID,
        academicYearId: UUID,
        excludeId: UUID
    ): Boolean = tenantDbQuery {
        StudentTransportAssignments.selectAll()
            .where {
                (StudentTransportAssignments.studentId eq studentId) and
                        (StudentTransportAssignments.academicYearId eq academicYearId) and
                        (StudentTransportAssignments.id neq excludeId)
            }
            .count() > 0
    }

    suspend fun toggleActiveStatus(id: UUID): Boolean = tenantDbQuery {
        val currentStatus = StudentTransportAssignments.selectAll()
            .where { StudentTransportAssignments.id eq id }
            .map { it[StudentTransportAssignments.isActive] }
            .singleOrNull()

        if (currentStatus != null) {
            StudentTransportAssignments.update({ StudentTransportAssignments.id eq id }) {
                it[isActive] = !currentStatus
            } > 0
        } else {
            false
        }
    }

    suspend fun bulkUpdateRouteAndStop(
        studentIds: List<UUID>,
        academicYearId: UUID,
        newRouteId: UUID,
        newStopId: UUID
    ): Int = tenantDbQuery {
        StudentTransportAssignments.update({
            (StudentTransportAssignments.studentId inList studentIds) and
                    (StudentTransportAssignments.academicYearId eq academicYearId)
        }) {
            it[routeId] = newRouteId
            it[stopId] = newStopId
        }
    }

    suspend fun deactivateAssignmentsByRouteId(routeId: UUID): Int = tenantDbQuery {
        StudentTransportAssignments.update({ StudentTransportAssignments.routeId eq routeId }) {
            it[isActive] = false
        }
    }

    suspend fun deactivateAssignmentsByStopId(stopId: UUID): Int = tenantDbQuery {
        StudentTransportAssignments.update({ StudentTransportAssignments.stopId eq stopId }) {
            it[isActive] = false
        }
    }

    private fun mapRowToDto(row: ResultRow): StudentTransportAssignmentDto {
        return StudentTransportAssignmentDto(
            id = row[StudentTransportAssignments.id].toString(),
            studentId = row[StudentTransportAssignments.studentId].toString(),
            studentName = "${row[Users.firstName]} ${row[Users.lastName]}",
            academicYearId = row[StudentTransportAssignments.academicYearId].toString(),
            academicYearName = row[AcademicYears.year],
            routeId = row[StudentTransportAssignments.routeId].toString(),
            routeName = row[TransportRoutes.name],
            stopId = row[StudentTransportAssignments.stopId].toString(),
            stopName = row[TransportStops.name],
            startDate = row[StudentTransportAssignments.startDate].toString(),
            endDate = row[StudentTransportAssignments.endDate]?.toString(),
            isActive = row[StudentTransportAssignments.isActive]
        )
    }
}