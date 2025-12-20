package com.example.repositories

import com.example.database.tables.LibrarySettings
import com.example.database.tables.Users
import com.example.models.dto.LibrarySettingsDto
import com.example.models.dto.UpdateLibrarySettingsRequest
import com.example.models.dto.UserSummaryDto
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.*

class LibrarySettingsRepository {

    suspend fun getSettings(): LibrarySettingsDto? = tenantDbQuery {
        val settings = LibrarySettings.selectAll().singleOrNull()
        settings?.let { mapRowToDto(it) }
    }

    suspend fun createDefaultSettings(): Int = tenantDbQuery {
        LibrarySettings.insert {
            // All defaults are set in the table definition
        } get LibrarySettings.id
    }

    suspend fun updateSettings(request: UpdateLibrarySettingsRequest, updatedBy: UUID): Boolean = tenantDbQuery {
        // Ensure settings exist
        val settingsId = LibrarySettings.selectAll().singleOrNull()?.get(LibrarySettings.id)
            ?: createDefaultSettings()

        LibrarySettings.update({ LibrarySettings.id eq settingsId }) {
            request.maxBooksPerStudent?.let { v -> it[maxBooksPerStudent] = v }
            request.maxBooksPerStaff?.let { v -> it[maxBooksPerStaff] = v }
            request.borrowingPeriodDays?.let { v -> it[borrowingPeriodDays] = v }
            request.maxRenewals?.let { v -> it[maxRenewals] = v }
            request.overdueFinePer Day?.let { v -> it[overdueFinePer Day] = BigDecimal(v) }
            request.lostBookFineMultiplier?.let { v -> it[lostBookFineMultiplier] = BigDecimal(v) }
            request.reservationExpiryDays?.let { v -> it[reservationExpiryDays] = v }
            request.enableReservations?.let { v -> it[enableReservations] = v }
            request.enableFines?.let { v -> it[enableFines] = v }
            it[updatedAt] = LocalDateTime.now()
            it[LibrarySettings.updatedBy] = updatedBy
        } > 0
    }

    suspend fun getOrCreateSettings(): LibrarySettingsDto = tenantDbQuery {
        var settings = LibrarySettings.selectAll().singleOrNull()

        if (settings == null) {
            createDefaultSettings()
            settings = LibrarySettings.selectAll().single()
        }

        mapRowToDto(settings)
    }

    private fun mapRowToDto(row: ResultRow): LibrarySettingsDto {
        return LibrarySettingsDto(
            id = row[LibrarySettings.id],
            maxBooksPerStudent = row[LibrarySettings.maxBooksPerStudent],
            maxBooksPerStaff = row[LibrarySettings.maxBooksPerStaff],
            borrowingPeriodDays = row[LibrarySettings.borrowingPeriodDays],
            maxRenewals = row[LibrarySettings.maxRenewals],
            overdueFinePer Day = row[LibrarySettings.overdueFinePer Day].toString(),
            lostBookFineMultiplier = row[LibrarySettings.lostBookFineMultiplier].toString(),
            reservationExpiryDays = row[LibrarySettings.reservationExpiryDays],
            enableReservations = row[LibrarySettings.enableReservations],
            enableFines = row[LibrarySettings.enableFines],
            updatedAt = row[LibrarySettings.updatedAt]?.toString(),
            updatedBy = null // TODO: Add when needed
        )
    }
}
