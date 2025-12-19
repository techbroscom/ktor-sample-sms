package com.example.models.dto

import com.example.database.tables.UserRole
import kotlinx.serialization.Serializable

@Serializable
data class DashboardOverviewDto(
    val totalStudents: Int,
    val totalStaff: Int,
    val totalClasses: Int,
    val totalSubjects: Int,
    val activeAcademicYears: Int,
    val upcomingExams: Int,
    val totalComplaints: Int,
    val pendingComplaints: Int,
    val todayAttendanceRate: Double,
    val totalHolidays: Int,
    val upcomingHolidays: Int,
    val storageUsed: StorageUsageDto? = null
)

@Serializable
data class StudentStatisticsDto(
    val totalStudents: Int,
    val studentsByClass: List<ClassStudentCountDto>,
    val studentsByAcademicYear: List<AcademicYearStudentCountDto>,
    val recentEnrollments: List<StudentEnrollmentDto>
)

@Serializable
data class ClassStudentCountDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val studentCount: Int,
    val academicYearName: String
)

@Serializable
data class AcademicYearStudentCountDto(
    val academicYearId: String,
    val academicYearName: String,
    val studentCount: Int
)

@Serializable
data class StudentEnrollmentDto(
    val studentId: String,
    val studentName: String,
    val className: String,
    val sectionName: String,
    val academicYearName: String,
    val enrollmentDate: String
)

@Serializable
data class StaffStatisticsDto(
    val totalStaff: Int,
    val staffByRole: List<StaffRoleCountDto>,
    val classTeachers: Int,
    val subjectTeachers: Int,
    val staffWorkload: List<StaffWorkloadDto>
)

@Serializable
data class StaffRoleCountDto(
    val role: UserRole,
    val count: Int
)

@Serializable
data class StaffWorkloadDto(
    val staffId: String,
    val staffName: String,
    val email: String,
    val classesAssigned: Int,
    val subjectsAssigned: Int,
    val totalWorkload: Int
)

@Serializable
data class ExamStatisticsDto(
    val totalExams: Int,
    val upcomingExams: Int,
    val examsBySubject: List<SubjectExamCountDto>,
    val examsByClass: List<ClassExamCountDto>,
    val recentExamResults: List<RecentExamResultDto>,
    val upcomingExamSchedules: List<UpcomingExamDto>
)

@Serializable
data class SubjectExamCountDto(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String?,
    val examCount: Int
)

@Serializable
data class ClassExamCountDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val examCount: Int,
    val academicYearName: String
)

@Serializable
data class RecentExamResultDto(
    val examId: String,
    val examName: String,
    val subjectName: String,
    val className: String,
    val sectionName: String,
    val averageMarks: Double,
    val maxMarks: Int,
    val studentsAppeared: Int,
    val examDate: String
)

@Serializable
data class UpcomingExamDto(
    val examId: String,
    val examName: String,
    val subjectName: String,
    val subjectCode: String?,
    val className: String,
    val sectionName: String,
    val examDate: String,
    val startTime: String,
    val endTime: String,
    val maxMarks: Int,
    val daysUntilExam: Int
)

@Serializable
data class AttendanceStatisticsDto(
    val todayAttendanceRate: Double,
    val weeklyAttendanceRate: Double,
    val monthlyAttendanceRate: Double,
    val attendanceByClass: List<ClassAttendanceStatDto>,
    val lowAttendanceStudents: List<StudentAttendanceStatDto>
)

@Serializable
data class ClassAttendanceStatDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val totalStudents: Int,
    val presentToday: Int,
    val attendanceRate: Double,
    val academicYearName: String
)

@Serializable
data class StudentAttendanceStatDto(
    val studentId: String,
    val studentName: String,
    val className: String,
    val sectionName: String,
    val attendanceRate: Double,
    val totalDays: Int,
    val presentDays: Int
)

@Serializable
data class ComplaintStatisticsDto(
    val totalComplaints: Int,
    val pendingComplaints: Int,
    val resolvedComplaints: Int,
    val complaintsByCategory: List<ComplaintCategoryCountDto>,
    val complaintsByStatus: List<ComplaintStatusCountDto>,
    val recentComplaints: List<RecentComplaintDto>
)

@Serializable
data class ComplaintCategoryCountDto(
    val category: String,
    val count: Int
)

@Serializable
data class ComplaintStatusCountDto(
    val status: String,
    val count: Int
)

@Serializable
data class RecentComplaintDto(
    val id: String,
    val title: String,
    val category: String,
    val status: String,
    val author: String,
    val isAnonymous: Boolean,
    val createdAt: String
)

@Serializable
data class AcademicStatisticsDto(
    val totalAcademicYears: Int,
    val activeAcademicYears: Int,
    val totalClasses: Int,
    val totalSubjects: Int,
    val classSubjectMappings: Int,
    val academicYearDetails: List<AcademicYearDetailDto>
)

@Serializable
data class AcademicYearDetailDto(
    val academicYearId: String,
    val academicYearName: String,
    val startDate: String,
    val endDate: String,
    val isActive: Boolean,
    val totalClasses: Int,
    val totalStudents: Int,
    val totalExams: Int
)

@Serializable
data class HolidayStatisticsDto(
    val totalHolidays: Int,
    val upcomingHolidays: Int,
    val publicHolidays: Int,
    val schoolHolidays: Int,
    val upcomingHolidaysList: List<UpcomingHolidayDto>
)

@Serializable
data class UpcomingHolidayDto(
    val id: Int,
    val name: String,
    val date: String,
    val description: String,
    val isPublicHoliday: Boolean,
    val daysUntilHoliday: Int
)

@Serializable
data class StudentCompleteDataDto(
    // Basic student information
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val mobileNumber: String,
    val createdAt: String,
    val updatedAt: String,

    // Academic assignments and current status
    val academicAssignments: List<StudentAcademicAssignmentDto>,
    val currentAcademicYear: StudentAcademicAssignmentDto?,
    val subjects: List<StudentSubjectDto>,

    // Attendance data
    val attendanceRecords: List<StudentUserAttendanceDto>,
    val attendanceStatistics: StudentAttendanceStatisticsDto,
    val recentAttendance: List<StudentUserAttendanceDto>,

    // Academic performance
    val examResults: List<StudentExamResultDto>,
    val academicPerformance: StudentAcademicPerformanceDto,
    val subjectPerformance: List<StudentSubjectPerformanceDto>,

    // Upcoming information
    val upcomingExams: List<StudentUpcomingExamDto>,
    val classTeachers: List<StudentClassTeacherDto>
)

@Serializable
data class StudentAcademicAssignmentDto(
    val assignmentId: String,
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String,
    val academicYearStartDate: String,
    val academicYearEndDate: String,
    val isActiveYear: Boolean
)

@Serializable
data class StudentSubjectDto(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String
)

@Serializable
data class StudentUserAttendanceDto(
    val attendanceId: String,
    val classId: String,
    val className: String,
    val sectionName: String,
    val date: String,
    val status: String
)

@Serializable
data class StudentAttendanceStatisticsDto(
    val totalDays: Int,
    val presentDays: Int,
    val absentDays: Int,
    val lateDays: Int,
    val attendancePercentage: Double,
    val recentAttendanceRate: Double
)

@Serializable
data class StudentExamResultDto(
    val resultId: String,
    val examId: String,
    val examName: String,
    val subjectName: String,
    val subjectCode: String,
    val className: String,
    val sectionName: String,
    val academicYearName: String,
    val examDate: String,
    val maxMarks: Int,
    val marksObtained: Int,
    val grade: String,
    val percentage: Double
)

@Serializable
data class StudentAcademicPerformanceDto(
    val totalExams: Int,
    val totalMarksObtained: Int,
    val totalMaxMarks: Int,
    val overallPercentage: Double
)

@Serializable
data class StudentSubjectPerformanceDto(
    val subjectName: String,
    val subjectCode: String,
    val academicYearName: String,
    val totalExams: Int,
    val totalMarksObtained: Int,
    val totalMaxMarks: Int,
    val averagePercentage: Double,
    val bestScore: Double,
    val worstScore: Double
)

@Serializable
data class StudentUpcomingExamDto(
    val examId: String,
    val examName: String,
    val subjectName: String,
    val subjectCode: String,
    val examDate: String,
    val maxMarks: Int,
    val startTime: String?,
    val endTime: String?,
    val daysUntilExam: Int
)

@Serializable
data class StudentClassTeacherDto(
    val teacherId: String,
    val teacherName: String,
    val email: String,
    val role: String
)

@Serializable
data class StudentBasicDataDto(
    // Basic student information
    val studentId: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val mobileNumber: String,
    val createdAt: String,
    val updatedAt: String,

    // Academic assignments and current status
    val academicAssignments: List<StudentAcademicAssignmentDto>,
    val currentAcademicYear: StudentAcademicAssignmentDto?,
    val subjects: List<StudentSubjectDto>,

    // Upcoming information
    val upcomingExams: List<StudentUpcomingExamDto>,
    val classTeachers: List<StudentClassTeacherDto>
)

@Serializable
data class StorageUsageDto(
    val totalBytes: Long,
    val totalMB: String,
    val totalGB: String,
    val tenantId: String
)

@Serializable
data class StorageStatisticsDto(
    val totalStorage: StorageUsageDto,
    val storageByModule: List<ModuleStorageDto>? = null
)

@Serializable
data class ModuleStorageDto(
    val module: String,
    val fileCount: Int,
    val totalBytes: Long,
    val totalMB: String
)