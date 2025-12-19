package com.example.services

import com.example.models.dto.*
import com.example.repositories.DashboardRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.supervisorScope
import services.S3FileService

class DashboardService(
    private val dashboardRepository: DashboardRepository,
    private val s3FileService: S3FileService?
) {

    /**
     * Get storage statistics for current tenant
     */
    suspend fun getStorageStatistics(tenantId: String): StorageStatisticsDto? {
        return try {
            if (s3FileService == null) return null

            val totalBytes = s3FileService.getTotalStorageByTenant(tenantId)
            val totalMB = totalBytes / (1024.0 * 1024.0)
            val totalGB = totalBytes / (1024.0 * 1024.0 * 1024.0)

            val storageUsage = StorageUsageDto(
                totalBytes = totalBytes,
                totalMB = String.format("%.2f", totalMB),
                totalGB = String.format("%.3f", totalGB),
                tenantId = tenantId
            )

            StorageStatisticsDto(
                totalStorage = storageUsage,
                storageByModule = null // Can be implemented later if needed
            )
        } catch (e: Exception) {
            println("Error getting storage statistics: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    /**
     * Get overall dashboard overview with key metrics
     */
    suspend fun getDashboardOverview(tenantId: String? = null): DashboardOverviewDto {
        return try {
            val overview = dashboardRepository.getDashboardOverview()

            // Add storage stats if tenantId is provided
            val storageUsage = if (tenantId != null) {
                getStorageStatistics(tenantId)?.totalStorage
            } else null

            overview.copy(storageUsed = storageUsage)
        } catch (e: Exception) {
            println("Error getting dashboard overview: ${e.message}")
            e.printStackTrace()
            // Return default values or re-throw based on your error handling strategy
            throw e
        }
    }

    /**
     * Get comprehensive student statistics
     */
    suspend fun getStudentStatistics(): StudentStatisticsDto {
        return try {
            dashboardRepository.getStudentStatistics()
        } catch (e: Exception) {
            println("Error getting student statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive student statistics
     */
    suspend fun getStudentStatistics(id: String): StudentCompleteDataDto? {
        println("Getting student statistics for id: $id")
        return try {
            dashboardRepository.getStudentCompleteData(id)
        } catch (e: Exception) {
            println("Error getting student statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive student statistics
     */
    suspend fun getStudentBasicStatistics(id: String): StudentBasicDataDto? {
        println("Getting student statistics for id: $id")
        return try {
            dashboardRepository.getStudentBasicData(id)
        } catch (e: Exception) {
            println("Error getting student statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive staff statistics
     */
    suspend fun getStaffStatistics(): StaffStatisticsDto {
        return try {
            dashboardRepository.getStaffStatistics()
        } catch (e: Exception) {
            println("Error getting staff statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive exam statistics
     */
    suspend fun getExamStatistics(): ExamStatisticsDto {
        return try {
            dashboardRepository.getExamStatistics()
        } catch (e: Exception) {
            println("Error getting exam statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive attendance statistics
     */
    suspend fun getAttendanceStatistics(): AttendanceStatisticsDto {
        return try {
            dashboardRepository.getAttendanceStatistics()
        } catch (e: Exception) {
            println("Error getting attendance statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive complaint statistics
     */
    suspend fun getComplaintStatistics(): ComplaintStatisticsDto {
        return try {
            dashboardRepository.getComplaintStatistics()
        } catch (e: Exception) {
            println("Error getting complaint statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive academic statistics
     */
    suspend fun getAcademicStatistics(): AcademicStatisticsDto {
        return try {
            dashboardRepository.getAcademicStatistics()
        } catch (e: Exception) {
            println("Error getting academic statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get comprehensive holiday statistics
     */
    suspend fun getHolidayStatistics(): HolidayStatisticsDto {
        return try {
            dashboardRepository.getHolidayStatistics()
        } catch (e: Exception) {
            println("Error getting holiday statistics: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Get complete dashboard data in a single call with parallel execution
     * Uses supervisorScope to ensure one failure doesn't cancel all operations
     */
    suspend fun getCompleteDashboard(tenantId: String? = null): CompleteDashboardDto = supervisorScope {
        try {
            val overviewDeferred = async { getDashboardOverview(tenantId) }
            val studentStatsDeferred = async { getStudentStatistics() }
            val staffStatsDeferred = async { getStaffStatistics() }
            val examStatsDeferred = async { getExamStatistics() }
            val attendanceStatsDeferred = async { getAttendanceStatistics() }
            val complaintStatsDeferred = async { getComplaintStatistics() }
            val academicStatsDeferred = async { getAcademicStatistics() }
            val holidayStatsDeferred = async { getHolidayStatistics() }
            val storageStatsDeferred = async {
                if (tenantId != null) getStorageStatistics(tenantId) else null
            }

            CompleteDashboardDto(
                overview = overviewDeferred.await(),
                studentStatistics = studentStatsDeferred.await(),
                staffStatistics = staffStatsDeferred.await(),
                examStatistics = examStatsDeferred.await(),
                attendanceStatistics = attendanceStatsDeferred.await(),
                complaintStatistics = complaintStatsDeferred.await(),
                academicStatistics = academicStatsDeferred.await(),
                holidayStatistics = holidayStatsDeferred.await(),
                storageStatistics = storageStatsDeferred.await()
            )
        } catch (e: Exception) {
            println("Error getting complete dashboard: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Alternative complete dashboard method that handles partial failures
     */
    suspend fun getCompleteDashboardSafe(): CompleteDashboardSafeDto = supervisorScope {
        val overviewDeferred = async {
            try { getDashboardOverview() } catch (e: Exception) {
                println("Failed to get overview: ${e.message}")
                null
            }
        }
        val studentStatsDeferred = async {
            try { getStudentStatistics() } catch (e: Exception) {
                println("Failed to get student stats: ${e.message}")
                null
            }
        }
        val staffStatsDeferred = async {
            try { getStaffStatistics() } catch (e: Exception) {
                println("Failed to get staff stats: ${e.message}")
                null
            }
        }
        val examStatsDeferred = async {
            try { getExamStatistics() } catch (e: Exception) {
                println("Failed to get exam stats: ${e.message}")
                null
            }
        }
        val attendanceStatsDeferred = async {
            try { getAttendanceStatistics() } catch (e: Exception) {
                println("Failed to get attendance stats: ${e.message}")
                null
            }
        }
        val complaintStatsDeferred = async {
            try { getComplaintStatistics() } catch (e: Exception) {
                println("Failed to get complaint stats: ${e.message}")
                null
            }
        }
        val academicStatsDeferred = async {
            try { getAcademicStatistics() } catch (e: Exception) {
                println("Failed to get academic stats: ${e.message}")
                null
            }
        }
        val holidayStatsDeferred = async {
            try { getHolidayStatistics() } catch (e: Exception) {
                println("Failed to get holiday stats: ${e.message}")
                null
            }
        }

        CompleteDashboardSafeDto(
            overview = overviewDeferred.await(),
            studentStatistics = studentStatsDeferred.await(),
            staffStatistics = staffStatsDeferred.await(),
            examStatistics = examStatsDeferred.await(),
            attendanceStatistics = attendanceStatsDeferred.await(),
            complaintStatistics = complaintStatsDeferred.await(),
            academicStatistics = academicStatsDeferred.await(),
            holidayStatistics = holidayStatsDeferred.await()
        )
    }
}

@kotlinx.serialization.Serializable
data class CompleteDashboardDto(
    val overview: DashboardOverviewDto,
    val studentStatistics: StudentStatisticsDto,
    val staffStatistics: StaffStatisticsDto,
    val examStatistics: ExamStatisticsDto,
    val attendanceStatistics: AttendanceStatisticsDto,
    val complaintStatistics: ComplaintStatisticsDto,
    val academicStatistics: AcademicStatisticsDto,
    val holidayStatistics: HolidayStatisticsDto,
    val storageStatistics: StorageStatisticsDto? = null
)

@kotlinx.serialization.Serializable
data class CompleteDashboardSafeDto(
    val overview: DashboardOverviewDto?,
    val studentStatistics: StudentStatisticsDto?,
    val staffStatistics: StaffStatisticsDto?,
    val examStatistics: ExamStatisticsDto?,
    val attendanceStatistics: AttendanceStatisticsDto?,
    val complaintStatistics: ComplaintStatisticsDto?,
    val academicStatistics: AcademicStatisticsDto?,
    val holidayStatistics: HolidayStatisticsDto?
)