package com.example.repositories

import com.example.database.tables.Users
import com.example.database.tables.VisitorStatus
import com.example.database.tables.Visitors
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class VisitorRepository {

    suspend fun create(request: CreateVisitorRequest, createdBy: UUID): UUID = tenantDbQuery {
        val visitorId = UUID.randomUUID()
        Visitors.insert {
            it[id] = visitorId
            it[firstName] = request.firstName
            it[lastName] = request.lastName
            it[email] = request.email
            it[mobileNumber] = request.mobileNumber
            it[organizationName] = request.organizationName
            it[purposeOfVisit] = request.purposeOfVisit
            it[visitDate] = LocalDate.parse(request.visitDate)
            it[expectedCheckInTime] = LocalDateTime.parse(request.expectedCheckInTime)
            it[status] = VisitorStatus.SCHEDULED
            it[hostUserId] = UUID.fromString(request.hostUserId)
            it[identificationProof] = request.identificationProof
            it[identificationNumber] = request.identificationNumber
            it[photoUrl] = request.photoUrl
            it[notes] = request.notes
            it[Visitors.createdBy] = createdBy
            it[createdAt] = LocalDateTime.now()
        }
        visitorId
    }

    suspend fun findById(visitorId: UUID): VisitorDto? = tenantDbQuery {
        val hostUserAlias = Users.alias("host_user")
        val createdByAlias = Users.alias("created_by_user")

        Visitors
            .join(hostUserAlias, JoinType.INNER, Visitors.hostUserId, hostUserAlias[Users.id])
            .join(createdByAlias, JoinType.INNER, Visitors.createdBy, createdByAlias[Users.id])
            .selectAll()
            .where { Visitors.id eq visitorId }
            .map { mapRowToDto(it, hostUserAlias, createdByAlias) }
            .singleOrNull()
    }

    suspend fun search(request: VisitorSearchRequest): Pair<List<VisitorDto>, Long> = tenantDbQuery {
        val hostUserAlias = Users.alias("host_user")
        val createdByAlias = Users.alias("created_by_user")

        var query = Visitors
            .join(hostUserAlias, JoinType.INNER, Visitors.hostUserId, hostUserAlias[Users.id])
            .join(createdByAlias, JoinType.INNER, Visitors.createdBy, createdByAlias[Users.id])
            .selectAll()

        // Apply filters
        request.searchQuery?.let { searchTerm ->
            query = query.andWhere {
                (Visitors.firstName like "%$searchTerm%") or
                (Visitors.lastName like "%$searchTerm%") or
                (Visitors.email like "%$searchTerm%") or
                (Visitors.mobileNumber like "%$searchTerm%")
            }
        }

        request.status?.let {
            query = query.andWhere { Visitors.status eq VisitorStatus.valueOf(it) }
        }

        request.hostUserId?.let {
            query = query.andWhere { Visitors.hostUserId eq UUID.fromString(it) }
        }

        request.visitDate?.let {
            query = query.andWhere { Visitors.visitDate eq LocalDate.parse(it) }
        }

        request.fromDate?.let {
            query = query.andWhere { Visitors.visitDate greaterEq LocalDate.parse(it) }
        }

        request.toDate?.let {
            query = query.andWhere { Visitors.visitDate lessEq LocalDate.parse(it) }
        }

        val total = query.count()
        val offset = ((request.page - 1) * request.pageSize).toLong()

        val visitors = query
            .limit(request.pageSize, offset)
            .orderBy(Visitors.visitDate to SortOrder.DESC, Visitors.expectedCheckInTime to SortOrder.DESC)
            .map { mapRowToDto(it, hostUserAlias, createdByAlias) }

        Pair(visitors, total)
    }

    suspend fun checkIn(visitorId: UUID, actualTime: LocalDateTime): Boolean = tenantDbQuery {
        Visitors.update({ Visitors.id eq visitorId }) {
            it[actualCheckInTime] = actualTime
            it[status] = VisitorStatus.CHECKED_IN
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun checkOut(visitorId: UUID, checkOutTime: LocalDateTime, notes: String?): Boolean = tenantDbQuery {
        Visitors.update({ Visitors.id eq visitorId }) {
            it[Visitors.checkOutTime] = checkOutTime
            it[status] = VisitorStatus.CHECKED_OUT
            notes?.let { noteText -> it[Visitors.notes] = noteText }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun update(visitorId: UUID, request: UpdateVisitorRequest): Boolean = tenantDbQuery {
        Visitors.update({ Visitors.id eq visitorId }) {
            request.firstName?.let { v -> it[firstName] = v }
            request.lastName?.let { v -> it[lastName] = v }
            request.email?.let { v -> it[email] = v }
            request.mobileNumber?.let { v -> it[mobileNumber] = v }
            request.organizationName?.let { v -> it[organizationName] = v }
            request.purposeOfVisit?.let { v -> it[purposeOfVisit] = v }
            request.visitDate?.let { v -> it[visitDate] = LocalDate.parse(v) }
            request.expectedCheckInTime?.let { v -> it[expectedCheckInTime] = LocalDateTime.parse(v) }
            request.hostUserId?.let { v -> it[hostUserId] = UUID.fromString(v) }
            request.notes?.let { v -> it[notes] = v }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun delete(visitorId: UUID): Boolean = tenantDbQuery {
        Visitors.deleteWhere { Visitors.id eq visitorId } > 0
    }

    suspend fun updateStatus(visitorId: UUID, newStatus: VisitorStatus): Boolean = tenantDbQuery {
        Visitors.update({ Visitors.id eq visitorId }) {
            it[status] = newStatus
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    suspend fun getStats(fromDate: LocalDate, toDate: LocalDate): VisitorStatsDto = tenantDbQuery {
        val total = Visitors.selectAll()
            .where { Visitors.visitDate.between(fromDate, toDate) }
            .count()

        val checkedIn = Visitors.selectAll()
            .where { Visitors.status eq VisitorStatus.CHECKED_IN }
            .count()

        val today = LocalDate.now()
        val scheduledToday = Visitors.selectAll()
            .where {
                (Visitors.visitDate eq today) and
                (Visitors.status eq VisitorStatus.SCHEDULED)
            }
            .count()

        val completedToday = Visitors.selectAll()
            .where {
                (Visitors.visitDate eq today) and
                (Visitors.status eq VisitorStatus.CHECKED_OUT)
            }
            .count()

        val weekAgo = today.minusDays(7)
        val noShows = Visitors.selectAll()
            .where {
                (Visitors.visitDate.between(weekAgo, today)) and
                (Visitors.status eq VisitorStatus.NO_SHOW)
            }
            .count()

        VisitorStatsDto(
            totalVisitors = total.toInt(),
            currentlyCheckedIn = checkedIn.toInt(),
            scheduledToday = scheduledToday.toInt(),
            completedToday = completedToday.toInt(),
            noShowsThisWeek = noShows.toInt()
        )
    }

    suspend fun findByHostUserId(hostUserId: UUID, status: String?): List<VisitorDto> = tenantDbQuery {
        val hostUserAlias = Users.alias("host_user")
        val createdByAlias = Users.alias("created_by_user")

        var query = Visitors
            .join(hostUserAlias, JoinType.INNER, Visitors.hostUserId, hostUserAlias[Users.id])
            .join(createdByAlias, JoinType.INNER, Visitors.createdBy, createdByAlias[Users.id])
            .selectAll()
            .where { Visitors.hostUserId eq hostUserId }

        status?.let {
            query = query.andWhere { Visitors.status eq VisitorStatus.valueOf(it) }
        }

        query
            .orderBy(Visitors.visitDate to SortOrder.DESC)
            .map { mapRowToDto(it, hostUserAlias, createdByAlias) }
    }

    private suspend fun getUserSummary(userId: UUID): UserSummaryDto = tenantDbQuery {
        Users.selectAll()
            .where { Users.id eq userId }
            .map {
                UserSummaryDto(
                    id = it[Users.id].toString(),
                    firstName = it[Users.firstName],
                    lastName = it[Users.lastName],
                    email = it[Users.email],
                    role = it[Users.role].name
                )
            }
            .single()
    }

    private fun mapRowToDto(
        row: ResultRow,
        hostUserAlias: Alias<Users>,
        createdByAlias: Alias<Users>
    ): VisitorDto {
        return VisitorDto(
            id = row[Visitors.id].toString(),
            firstName = row[Visitors.firstName],
            lastName = row[Visitors.lastName],
            fullName = "${row[Visitors.firstName]} ${row[Visitors.lastName]}",
            email = row[Visitors.email],
            mobileNumber = row[Visitors.mobileNumber],
            organizationName = row[Visitors.organizationName],
            purposeOfVisit = row[Visitors.purposeOfVisit],
            visitDate = row[Visitors.visitDate].toString(),
            expectedCheckInTime = row[Visitors.expectedCheckInTime].toString(),
            actualCheckInTime = row[Visitors.actualCheckInTime]?.toString(),
            checkOutTime = row[Visitors.checkOutTime]?.toString(),
            status = row[Visitors.status].name,
            hostUser = UserSummaryDto(
                id = row[hostUserAlias[Users.id]].toString(),
                firstName = row[hostUserAlias[Users.firstName]],
                lastName = row[hostUserAlias[Users.lastName]],
                email = row[hostUserAlias[Users.email]],
                role = row[hostUserAlias[Users.role]].name
            ),
            identificationProof = row[Visitors.identificationProof],
            identificationNumber = row[Visitors.identificationNumber],
            photoUrl = row[Visitors.photoUrl],
            notes = row[Visitors.notes],
            createdAt = row[Visitors.createdAt].toString(),
            updatedAt = row[Visitors.updatedAt]?.toString(),
            createdBy = UserSummaryDto(
                id = row[createdByAlias[Users.id]].toString(),
                firstName = row[createdByAlias[Users.firstName]],
                lastName = row[createdByAlias[Users.lastName]],
                email = row[createdByAlias[Users.email]],
                role = row[createdByAlias[Users.role]].name
            )
        )
    }
}
