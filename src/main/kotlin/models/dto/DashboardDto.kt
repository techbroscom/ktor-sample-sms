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
    val upcomingHolidays: Int
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