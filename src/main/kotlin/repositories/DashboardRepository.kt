package com.example.repositories

import com.example.database.tables.*
import com.example.models.dto.*
import com.example.utils.tenantDbQuery
import com.example.utils.tenantDbQuery
import org.jetbrains.exposed.sql.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID


class DashboardRepository {

    suspend fun getDashboardOverview(): DashboardOverviewDto = tenantDbQuery {
        val today = LocalDate.now()
        val todayDateTime = LocalDateTime.now()

        val totalStudents = Users.selectAll()
            .where { Users.role eq UserRole.STUDENT }
            .count().toInt()

        val totalStaff = Users.selectAll()
            .where { Users.role inList listOf(UserRole.ADMIN, UserRole.STAFF) }
            .count().toInt()

        val totalClasses = Classes.selectAll().count().toInt()

        val totalSubjects = Subjects.selectAll().count().toInt()

        val activeAcademicYears = AcademicYears.selectAll()
            .where { AcademicYears.isActive eq true }
            .count().toInt()

        val upcomingExams = Exams.selectAll()
            .where { Exams.date greaterEq today }
            .count().toInt()

        val totalComplaints = Complaints.selectAll().count().toInt()

        val pendingComplaints = Complaints.selectAll()
            .where { Complaints.status neq "RESOLVED" }
            .count().toInt()

        // Fixed: Safer attendance calculation with proper single where clause
        val totalStudentsToday = Attendance.selectAll()
            .where { Attendance.date eq today }
            .count().toInt()

        val presentToday = Attendance.selectAll()
            .where { (Attendance.date eq today) and (Attendance.status eq AttendanceStatus.PRESENT) }
            .count().toInt()

        val todayAttendanceRate = if (totalStudentsToday > 0) {
            (presentToday.toDouble() / totalStudentsToday) * 100
        } else 0.0

        val totalHolidays = Holidays.selectAll().count().toInt()

        val upcomingHolidays = Holidays.selectAll()
            .where { Holidays.date greaterEq today }
            .count().toInt()

        DashboardOverviewDto(
            totalStudents = totalStudents,
            totalStaff = totalStaff,
            totalClasses = totalClasses,
            totalSubjects = totalSubjects,
            activeAcademicYears = activeAcademicYears,
            upcomingExams = upcomingExams,
            totalComplaints = totalComplaints,
            pendingComplaints = pendingComplaints,
            todayAttendanceRate = todayAttendanceRate,
            totalHolidays = totalHolidays,
            upcomingHolidays = upcomingHolidays
        )
    }

    suspend fun getAttendanceStatistics(): AttendanceStatisticsDto = tenantDbQuery {
        val today = LocalDate.now()
        val weekStart = today.minusDays(7)
        val monthStart = today.minusDays(30)

        // Today's attendance - Fixed: Combined conditions in single where clause
        val totalToday = Attendance.selectAll()
            .where { Attendance.date eq today }
            .count().toInt()

        val presentToday = Attendance.selectAll()
            .where { (Attendance.date eq today) and (Attendance.status eq AttendanceStatus.PRESENT) }
            .count().toInt()

        val todayRate = if (totalToday > 0) {
            (presentToday.toDouble() / totalToday) * 100
        } else 0.0

        // Weekly attendance - Fixed: Combined conditions in single where clause
        val totalWeekly = Attendance.selectAll()
            .where { Attendance.date greaterEq weekStart }
            .count().toInt()

        val presentWeekly = Attendance.selectAll()
            .where { (Attendance.date greaterEq weekStart) and (Attendance.status eq AttendanceStatus.PRESENT) }
            .count().toInt()

        val weeklyRate = if (totalWeekly > 0) {
            (presentWeekly.toDouble() / totalWeekly) * 100
        } else 0.0

        // Monthly attendance - Fixed: Combined conditions in single where clause
        val totalMonthly = Attendance.selectAll()
            .where { Attendance.date greaterEq monthStart }
            .count().toInt()

        val presentMonthly = Attendance.selectAll()
            .where { (Attendance.date greaterEq monthStart) and (Attendance.status eq AttendanceStatus.PRESENT) }
            .count().toInt()

        val monthlyRate = if (totalMonthly > 0) {
            (presentMonthly.toDouble() / totalMonthly) * 100
        } else 0.0

        // Fixed: Proper attendance by class calculation with error handling
        val attendanceByClassRaw = try {
            Attendance
                .join(Classes, JoinType.INNER, Attendance.classId, Classes.id)
                .join(AcademicYears, JoinType.INNER, Classes.academicYearId, AcademicYears.id)
                .selectAll()
                .where { Attendance.date eq today }
                .mapNotNull { row ->
                    try {
                        ClassAttendanceStatDto(
                            classId = row[Classes.id].toString(),
                            className = row[Classes.className],
                            sectionName = row[Classes.sectionName] ?: "",
                            totalStudents = 1,
                            presentToday = if (row[Attendance.status] == AttendanceStatus.PRESENT) 1 else 0,
                            attendanceRate = 0.0,
                            academicYearName = row[AcademicYears.year] ?: ""
                        )
                    } catch (e: Exception) {
                        null // Skip problematic rows
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }

        val attendanceByClass = attendanceByClassRaw
            .groupBy { "${it.classId}_${it.academicYearName}" }
            .map { (_, dtos) ->
                val first = dtos.first()
                val total = dtos.size
                val present = dtos.sumOf { it.presentToday }
                val rate = if (total > 0) (present.toDouble() / total) * 100 else 0.0

                first.copy(
                    totalStudents = total,
                    presentToday = present,
                    attendanceRate = rate
                )
            }

        // Fixed: Proper low attendance students calculation with error handling
        val lowAttendanceStudentsRaw = try {
            Attendance
                .join(Users, JoinType.INNER, Attendance.studentId, Users.id)
                .join(Classes, JoinType.INNER, Attendance.classId, Classes.id)
                .selectAll()
                .where { Attendance.date greaterEq monthStart }
                .mapNotNull { row ->
                    try {
                        StudentAttendanceStatDto(
                            studentId = row[Users.id].toString(),
                            studentName = "${row[Users.firstName]} ${row[Users.lastName]}",
                            className = row[Classes.className],
                            sectionName = row[Classes.sectionName] ?: "",
                            attendanceRate = 0.0,
                            totalDays = 1,
                            presentDays = if (row[Attendance.status] == AttendanceStatus.PRESENT) 1 else 0
                        )
                    } catch (e: Exception) {
                        null // Skip problematic rows
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }

        val lowAttendanceStudents = lowAttendanceStudentsRaw
            .groupBy { it.studentId }
            .mapNotNull { (_, dtos) ->
                try {
                    val first = dtos.first()
                    val total = dtos.size
                    val present = dtos.sumOf { it.presentDays }
                    val rate = if (total > 0) (present.toDouble() / total) * 100 else 0.0

                    if (rate < 75.0 && total >= 10) {
                        first.copy(
                            attendanceRate = rate,
                            totalDays = total,
                            presentDays = present
                        )
                    } else null
                } catch (e: Exception) {
                    null
                }
            }
            .sortedBy { it.attendanceRate }
            .take(10)

        AttendanceStatisticsDto(
            todayAttendanceRate = todayRate,
            weeklyAttendanceRate = weeklyRate,
            monthlyAttendanceRate = monthlyRate,
            attendanceByClass = attendanceByClass,
            lowAttendanceStudents = lowAttendanceStudents
        )
    }

    suspend fun getStudentStatistics(): StudentStatisticsDto = tenantDbQuery {
        val totalStudents = Users.selectAll()
            .where { Users.role eq UserRole.STUDENT }
            .count().toInt()

        // Fixed: Proper groupBy with Exposed SQL
        val studentsByClass = StudentAssignments
            .join(Classes, JoinType.INNER, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Classes.className)
            .map { row ->
                ClassStudentCountDto(
                    classId = row[Classes.id].toString(),
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName],
                    studentCount = 1, // This needs to be calculated differently - see note below
                    academicYearName = row[AcademicYears.year] ?: ""
                )
            }
            .groupBy { "${it.classId}_${it.academicYearName}" }
            .map { (_, dtos) ->
                val first = dtos.first()
                first.copy(studentCount = dtos.size)
            }

        val studentsByAcademicYear = StudentAssignments
            .join(AcademicYears, JoinType.INNER, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .map { row ->
                AcademicYearStudentCountDto(
                    academicYearId = row[AcademicYears.id].toString(),
                    academicYearName = row[AcademicYears.year] ?: "",
                    studentCount = 1 // This needs to be calculated differently
                )
            }
            .groupBy { it.academicYearId }
            .map { (_, dtos) ->
                val first = dtos.first()
                first.copy(studentCount = dtos.size)
            }

        val recentEnrollments = StudentAssignments
            .join(Users, JoinType.INNER, StudentAssignments.studentId, Users.id)
            .join(Classes, JoinType.INNER, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Users.createdAt to SortOrder.DESC)
            .limit(10)
            .map { row ->
                StudentEnrollmentDto(
                    studentId = row[StudentAssignments.studentId].toString(),
                    studentName = "${row[Users.firstName]} ${row[Users.lastName]}",
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName],
                    academicYearName = row[AcademicYears.year] ?: "",
                    enrollmentDate = row[Users.createdAt].toString()
                )
            }

        StudentStatisticsDto(
            totalStudents = totalStudents,
            studentsByClass = studentsByClass,
            studentsByAcademicYear = studentsByAcademicYear,
            recentEnrollments = recentEnrollments
        )
    }

    suspend fun getStaffStatistics(): StaffStatisticsDto = tenantDbQuery {
        val totalStaff = Users.selectAll()
            .where { Users.role inList listOf(UserRole.ADMIN, UserRole.STAFF) }
            .count().toInt()

        val staffByRole = Users.selectAll()
            .where { Users.role inList listOf(UserRole.ADMIN, UserRole.STAFF) }
            .map { row ->
                StaffRoleCountDto(
                    role = row[Users.role],
                    count = 1
                )
            }
            .groupBy { it.role }
            .map { (role, dtos) ->
                StaffRoleCountDto(
                    role = role,
                    count = dtos.size
                )
            }

        val classTeachers = StaffClassAssignments.selectAll()
            .where { StaffClassAssignments.role eq StaffClassRole.CLASS_TEACHER }
            .count().toInt()

        val subjectTeachers = StaffSubjectAssignments.selectAll()
            .count().toInt()

        // Fixed: Simplified staff workload calculation
        val staffWorkload = Users.selectAll()
            .where { Users.role inList listOf(UserRole.ADMIN, UserRole.STAFF) }
            .limit(10)
            .map { userRow ->
                val staffId = userRow[Users.id]

                val classCount = StaffClassAssignments.selectAll()
                    .where { StaffClassAssignments.staffId eq staffId }
                    .count().toInt()

                val subjectCount = StaffSubjectAssignments.selectAll()
                    .where { StaffSubjectAssignments.staffId eq staffId }
                    .count().toInt()

                StaffWorkloadDto(
                    staffId = staffId.toString(),
                    staffName = "${userRow[Users.firstName]} ${userRow[Users.lastName]}",
                    email = userRow[Users.email],
                    classesAssigned = classCount,
                    subjectsAssigned = subjectCount,
                    totalWorkload = classCount + subjectCount
                )
            }
            .sortedByDescending { it.totalWorkload }
            .take(10)

        StaffStatisticsDto(
            totalStaff = totalStaff,
            staffByRole = staffByRole,
            classTeachers = classTeachers,
            subjectTeachers = subjectTeachers,
            staffWorkload = staffWorkload
        )
    }

    suspend fun getExamStatistics(): ExamStatisticsDto = tenantDbQuery {
        val today = LocalDate.now()

        val totalExams = Exams.selectAll().count().toInt()

        val upcomingExams = Exams.selectAll()
            .where { Exams.date greaterEq today }
            .count().toInt()

        val examsBySubject = Exams
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .selectAll()
            .map { row ->
                SubjectExamCountDto(
                    subjectId = row[Subjects.id].toString(),
                    subjectName = row[Subjects.name],
                    subjectCode = row[Subjects.code],
                    examCount = 1
                )
            }
            .groupBy { it.subjectId }
            .map { (_, dtos) ->
                val first = dtos.first()
                first.copy(examCount = dtos.size)
            }

        val examsByClass = Exams
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .map { row ->
                ClassExamCountDto(
                    classId = row[Classes.id].toString(),
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName],
                    examCount = 1,
                    academicYearName = row[AcademicYears.year] ?: ""
                )
            }
            .groupBy { it.classId }
            .map { (_, dtos) ->
                val first = dtos.first()
                first.copy(examCount = dtos.size)
            }

        // Fixed: Proper exam results calculation
        val recentExamResults = Exams
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .selectAll()
            .where { Exams.date lessEq today }
            .orderBy(Exams.date to SortOrder.DESC)
            .limit(10)
            .map { examRow ->
                val examId = examRow[Exams.id]

                val results = ExamResults.selectAll()
                    .where { ExamResults.examId eq examId }

                val marks = results.map { it[ExamResults.marksObtained] }
                val averageMarks = if (marks.isNotEmpty()) marks.average() else 0.0

                RecentExamResultDto(
                    examId = examId.toString(),
                    examName = examRow[Exams.name],
                    subjectName = examRow[Subjects.name],
                    className = examRow[Classes.className],
                    sectionName = examRow[Classes.sectionName],
                    averageMarks = averageMarks,
                    maxMarks = examRow[Exams.maxMarks],
                    studentsAppeared = results.count().toInt(),
                    examDate = examRow[Exams.date].toString()
                )
            }

        val upcomingExamSchedules = ExamSchedules
            .join(Exams, JoinType.INNER, ExamSchedules.examId, Exams.id)
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, ExamSchedules.classId, Classes.id)
            .selectAll()
            .where { Exams.date greaterEq today }
            .orderBy(Exams.date to SortOrder.ASC, ExamSchedules.startTime to SortOrder.ASC)
            .limit(10)
            .map { row ->
                val examDate = row[Exams.date]
                val daysUntil = ChronoUnit.DAYS.between(today, examDate).toInt()

                UpcomingExamDto(
                    examId = row[Exams.id].toString(),
                    examName = row[Exams.name],
                    subjectName = row[Subjects.name],
                    subjectCode = row[Subjects.code],
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName],
                    examDate = examDate.toString(),
                    startTime = row[ExamSchedules.startTime].toString(),
                    endTime = row[ExamSchedules.endTime].toString(),
                    maxMarks = row[Exams.maxMarks],
                    daysUntilExam = daysUntil
                )
            }

        ExamStatisticsDto(
            totalExams = totalExams,
            upcomingExams = upcomingExams,
            examsBySubject = examsBySubject,
            examsByClass = examsByClass,
            recentExamResults = recentExamResults,
            upcomingExamSchedules = upcomingExamSchedules
        )
    }

    suspend fun getComplaintStatistics(): ComplaintStatisticsDto = tenantDbQuery {
        val totalComplaints = Complaints.selectAll().count().toInt()

        val pendingComplaints = Complaints.selectAll()
            .where { Complaints.status neq "RESOLVED" }
            .count().toInt()

        val resolvedComplaints = Complaints.selectAll()
            .where { Complaints.status eq "RESOLVED" }
            .count().toInt()

        val complaintsByCategory = Complaints.selectAll()
            .map { row ->
                ComplaintCategoryCountDto(
                    category = row[Complaints.category] ?: "Unknown",
                    count = 1
                )
            }
            .groupBy { it.category }
            .map { (category, dtos) ->
                ComplaintCategoryCountDto(
                    category = category,
                    count = dtos.size
                )
            }

        val complaintsByStatus = Complaints.selectAll()
            .map { row ->
                ComplaintStatusCountDto(
                    status = row[Complaints.status] ?: "Unknown",
                    count = 1
                )
            }
            .groupBy { it.status }
            .map { (status, dtos) ->
                ComplaintStatusCountDto(
                    status = status,
                    count = dtos.size
                )
            }

        val recentComplaints = Complaints.selectAll()
            .orderBy(Complaints.createdAt to SortOrder.DESC)
            .limit(10)
            .map { row ->
                RecentComplaintDto(
                    id = row[Complaints.id],
                    title = row[Complaints.title] ?: "",
                    category = row[Complaints.category] ?: "",
                    status = row[Complaints.status] ?: "",
                    author = row[Complaints.author] ?: "",
                    isAnonymous = row[Complaints.isAnonymous],
                    createdAt = row[Complaints.createdAt].toString()
                )
            }

        ComplaintStatisticsDto(
            totalComplaints = totalComplaints,
            pendingComplaints = pendingComplaints,
            resolvedComplaints = resolvedComplaints,
            complaintsByCategory = complaintsByCategory,
            complaintsByStatus = complaintsByStatus,
            recentComplaints = recentComplaints
        )
    }

    suspend fun getAcademicStatistics(): AcademicStatisticsDto = tenantDbQuery {
        val totalAcademicYears = AcademicYears.selectAll().count().toInt()

        val activeAcademicYears = AcademicYears.selectAll()
            .where { AcademicYears.isActive eq true }
            .count().toInt()

        val totalClasses = Classes.selectAll().count().toInt()

        val totalSubjects = Subjects.selectAll().count().toInt()

        val classSubjectMappings = ClassSubjects.selectAll().count().toInt()

        val academicYearDetails = AcademicYears.selectAll()
            .orderBy(AcademicYears.year to SortOrder.DESC)
            .map { row ->
                val yearId = row[AcademicYears.id]

                val classCount = Classes.selectAll()
                    .where { Classes.academicYearId eq yearId }
                    .count().toInt()

                val studentCount = StudentAssignments.selectAll()
                    .where { StudentAssignments.academicYearId eq yearId }
                    .count().toInt()

                val examCount = Exams.selectAll()
                    .where { Exams.academicYearId eq yearId }
                    .count().toInt()

                AcademicYearDetailDto(
                    academicYearId = yearId.toString(),
                    academicYearName = row[AcademicYears.year] ?: "",
                    startDate = row[AcademicYears.startDate].toString(),
                    endDate = row[AcademicYears.endDate].toString(),
                    isActive = row[AcademicYears.isActive],
                    totalClasses = classCount,
                    totalStudents = studentCount,
                    totalExams = examCount
                )
            }

        AcademicStatisticsDto(
            totalAcademicYears = totalAcademicYears,
            activeAcademicYears = activeAcademicYears,
            totalClasses = totalClasses,
            totalSubjects = totalSubjects,
            classSubjectMappings = classSubjectMappings,
            academicYearDetails = academicYearDetails
        )
    }

    suspend fun getHolidayStatistics(): HolidayStatisticsDto = tenantDbQuery {
        val today = LocalDate.now()

        val totalHolidays = Holidays.selectAll().count().toInt()

        val upcomingHolidays = Holidays.selectAll()
            .where { Holidays.date greaterEq today }
            .count().toInt()

        val publicHolidays = Holidays.selectAll()
            .where { Holidays.isPublicHoliday eq true }
            .count().toInt()

        val schoolHolidays = Holidays.selectAll()
            .where { Holidays.isPublicHoliday eq false }
            .count().toInt()

        val upcomingHolidaysList = Holidays.selectAll()
            .where { Holidays.date greaterEq today }
            .orderBy(Holidays.date to SortOrder.ASC)
            .limit(10)
            .map { row ->
                val holidayDate = row[Holidays.date]
                val daysUntil = ChronoUnit.DAYS.between(today, holidayDate).toInt()

                UpcomingHolidayDto(
                    id = row[Holidays.id],
                    name = row[Holidays.name],
                    date = holidayDate.toString(),
                    description = row[Holidays.description] ?: "",
                    isPublicHoliday = row[Holidays.isPublicHoliday],
                    daysUntilHoliday = daysUntil
                )
            }

        HolidayStatisticsDto(
            totalHolidays = totalHolidays,
            upcomingHolidays = upcomingHolidays,
            publicHolidays = publicHolidays,
            schoolHolidays = schoolHolidays,
            upcomingHolidaysList = upcomingHolidaysList
        )
    }

    suspend fun getStudentCompleteData(studentId: String): StudentCompleteDataDto? = tenantDbQuery {
        // First, verify the student exists and get basic info
        val studentInfo = Users.selectAll()
            .where { (Users.id eq UUID.fromString(studentId)) and (Users.role eq UserRole.STUDENT) }
            .singleOrNull() ?: return@tenantDbQuery null

        // Get student's academic assignments (classes across all years)
        val academicAssignments = StudentAssignments
            .join(Classes, JoinType.INNER, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.studentId eq UUID.fromString(studentId) }
            .orderBy(AcademicYears.startDate to SortOrder.DESC)
            .map { row ->
                StudentAcademicAssignmentDto(
                    assignmentId = row[StudentAssignments.id].toString(),
                    classId = row[Classes.id].toString(),
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName] ?: "",
                    academicYearId = row[AcademicYears.id].toString(),
                    academicYearName = row[AcademicYears.year] ?: "",
                    academicYearStartDate = row[AcademicYears.startDate].toString(),
                    academicYearEndDate = row[AcademicYears.endDate].toString(),
                    isActiveYear = row[AcademicYears.isActive]
                )
            }

        // Get subjects for each class the student is/was enrolled in
        val subjectsData = mutableListOf<StudentSubjectDto>()
        academicAssignments.forEach { assignment ->
            val subjects = ClassSubjects
                .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
                .selectAll()
                .where {
                    (ClassSubjects.classId eq UUID.fromString(assignment.classId)) and
                            (ClassSubjects.academicYearId eq UUID.fromString(assignment.academicYearId))
                }
                .map { row ->
                    StudentSubjectDto(
                        subjectId = row[Subjects.id].toString(),
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code]?:"",
                        classId = assignment.classId,
                        className = assignment.className,
                        sectionName = assignment.sectionName,
                        academicYearId = assignment.academicYearId,
                        academicYearName = assignment.academicYearName
                    )
                }
            subjectsData.addAll(subjects)
        }

        // Get attendance records
        val attendanceRecords = Attendance
            .join(Classes, JoinType.INNER, Attendance.classId, Classes.id)
            .selectAll()
            .where { Attendance.studentId eq UUID.fromString(studentId) }
            .orderBy(Attendance.date to SortOrder.DESC)
            .map { row ->
                StudentUserAttendanceDto(
                    attendanceId = row[Attendance.id].toString(),
                    classId = row[Classes.id].toString(),
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName] ?: "",
                    date = row[Attendance.date].toString(),
                    status = row[Attendance.status].toString()
                )
            }

        // Calculate attendance statistics
        val totalAttendanceDays = attendanceRecords.size
        val presentDays = attendanceRecords.count { it.status == "PRESENT" }
        val absentDays = attendanceRecords.count { it.status == "ABSENT" }
        val lateDays = attendanceRecords.count { it.status == "LATE" }
        val attendancePercentage = if (totalAttendanceDays > 0) {
            (presentDays.toDouble() / totalAttendanceDays) * 100
        } else 0.0

        // Get exam results with exam details
        val examResults = ExamResults
            .join(Exams, JoinType.INNER, ExamResults.examId, Exams.id)
            .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
            .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, Exams.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ExamResults.studentId eq UUID.fromString(studentId) }
            .orderBy(Exams.date to SortOrder.DESC)
            .map { row ->
                StudentExamResultDto(
                    resultId = row[ExamResults.id].toString(),
                    examId = row[Exams.id].toString(),
                    examName = row[Exams.name],
                    subjectName = row[Subjects.name],
                    subjectCode = row[Subjects.code]?:"",
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName] ?: "",
                    academicYearName = row[AcademicYears.year] ?: "",
                    examDate = row[Exams.date].toString(),
                    maxMarks = row[Exams.maxMarks],
                    marksObtained = row[ExamResults.marksObtained],
                    grade = row[ExamResults.grade] ?: "",
                    percentage = if (row[Exams.maxMarks] > 0) {
                        (row[ExamResults.marksObtained].toDouble() / row[Exams.maxMarks]) * 100
                    } else 0.0
                )
            }

        // Calculate academic performance statistics
        val totalExams = examResults.size
        val totalMarksObtained = examResults.sumOf { it.marksObtained }
        val totalMaxMarks = examResults.sumOf { it.maxMarks }
        val overallPercentage = if (totalMaxMarks > 0) {
            (totalMarksObtained.toDouble() / totalMaxMarks) * 100
        } else 0.0

        // Get subject-wise performance
        val subjectPerformance = examResults
            .groupBy { "${it.subjectCode}_${it.academicYearName}" }
            .map { (_, results) ->
                val subjectResults = results.first()
                val subjectTotal = results.sumOf { it.marksObtained }
                val subjectMax = results.sumOf { it.maxMarks }
                val subjectPercentage = if (subjectMax > 0) {
                    (subjectTotal.toDouble() / subjectMax) * 100
                } else 0.0

                StudentSubjectPerformanceDto(
                    subjectName = subjectResults.subjectName,
                    subjectCode = subjectResults.subjectCode,
                    academicYearName = subjectResults.academicYearName,
                    totalExams = results.size,
                    totalMarksObtained = subjectTotal,
                    totalMaxMarks = subjectMax,
                    averagePercentage = subjectPercentage,
                    bestScore = results.maxOfOrNull { it.percentage } ?: 0.0,
                    worstScore = results.minOfOrNull { it.percentage } ?: 0.0
                )
            }
            .sortedByDescending { it.averagePercentage }

        // Get upcoming exams for current academic year
        val today = LocalDate.now()
        val currentAcademicYear = academicAssignments.find { it.isActiveYear }

        val upcomingExams = if (currentAcademicYear != null) {
            Exams
                .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
                .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
                .join(ExamSchedules, JoinType.LEFT, Exams.id, ExamSchedules.examId)
                .selectAll()
                .where {
                    (Exams.classId eq UUID.fromString(currentAcademicYear.classId)) and
                            (Exams.academicYearId eq UUID.fromString(currentAcademicYear.academicYearId)) and
                            (Exams.date greaterEq today)
                }
                .orderBy(Exams.date to SortOrder.ASC)
                .map { row ->
                    StudentUpcomingExamDto(
                        examId = row[Exams.id].toString(),
                        examName = row[Exams.name],
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code]?:"",
                        examDate = row[Exams.date].toString(),
                        maxMarks = row[Exams.maxMarks],
                        startTime = row.getOrNull(ExamSchedules.startTime)?.toString(),
                        endTime = row.getOrNull(ExamSchedules.endTime)?.toString(),
                        daysUntilExam = ChronoUnit.DAYS.between(today, row[Exams.date]).toInt()
                    )
                }
        } else emptyList()

        // Get recent attendance (last 30 days)
        val thirtyDaysAgo = today.minusDays(30)
        val recentAttendance = attendanceRecords
            .filter {
                try {
                    LocalDate.parse(it.date) >= thirtyDaysAgo
                } catch (e: Exception) {
                    false
                }
            }
            .take(30)

        val recentAttendanceRate = if (recentAttendance.isNotEmpty()) {
            (recentAttendance.count { it.status == "PRESENT" }.toDouble() / recentAttendance.size) * 100
        } else 0.0

        // Get class teachers for current class
        val classTeachers = if (currentAcademicYear != null) {
            StaffClassAssignments
                .join(Users, JoinType.INNER, StaffClassAssignments.staffId, Users.id)
                .selectAll()
                .where {
                    (StaffClassAssignments.classId eq UUID.fromString(currentAcademicYear.classId)) and
                            (StaffClassAssignments.academicYearId eq UUID.fromString(currentAcademicYear.academicYearId))
                }
                .map { row ->
                    StudentClassTeacherDto(
                        teacherId = row[Users.id].toString(),
                        teacherName = "${row[Users.firstName]} ${row[Users.lastName]}",
                        email = row[Users.email],
                        role = row[StaffClassAssignments.role].toString()
                    )
                }
        } else emptyList()

        StudentCompleteDataDto(
            // Basic student information
            studentId = studentInfo[Users.id].toString(),
            firstName = studentInfo[Users.firstName],
            lastName = studentInfo[Users.lastName],
            email = studentInfo[Users.email],
            mobileNumber = studentInfo[Users.mobileNumber] ?: "",
            createdAt = studentInfo[Users.createdAt].toString(),
            updatedAt = studentInfo[Users.updatedAt].toString(),

            // Academic assignments and current status
            academicAssignments = academicAssignments,
            currentAcademicYear = currentAcademicYear,
            subjects = subjectsData,

            // Attendance data
            attendanceRecords = attendanceRecords,
            attendanceStatistics = StudentAttendanceStatisticsDto(
                totalDays = totalAttendanceDays,
                presentDays = presentDays,
                absentDays = absentDays,
                lateDays = lateDays,
                attendancePercentage = attendancePercentage,
                recentAttendanceRate = recentAttendanceRate
            ),
            recentAttendance = recentAttendance,

            // Academic performance
            examResults = examResults,
            academicPerformance = StudentAcademicPerformanceDto(
                totalExams = totalExams,
                totalMarksObtained = totalMarksObtained,
                totalMaxMarks = totalMaxMarks,
                overallPercentage = overallPercentage
            ),
            subjectPerformance = subjectPerformance,

            // Upcoming information
            upcomingExams = upcomingExams,
            classTeachers = classTeachers
        )
    }

    suspend fun getStudentBasicData(studentId: String): StudentBasicDataDto? = tenantDbQuery {
        // First, verify the student exists and get basic info
        val studentInfo = Users.selectAll()
            .where { (Users.id eq UUID.fromString(studentId)) and (Users.role eq UserRole.STUDENT) }
            .singleOrNull() ?: return@tenantDbQuery null

        // Get student's academic assignments (classes across all years)
        val academicAssignments = StudentAssignments
            .join(Classes, JoinType.INNER, StudentAssignments.classId, Classes.id)
            .join(AcademicYears, JoinType.INNER, StudentAssignments.academicYearId, AcademicYears.id)
            .selectAll()
            .where { StudentAssignments.studentId eq UUID.fromString(studentId) }
            .orderBy(AcademicYears.startDate to SortOrder.DESC)
            .map { row ->
                StudentAcademicAssignmentDto(
                    assignmentId = row[StudentAssignments.id].toString(),
                    classId = row[Classes.id].toString(),
                    className = row[Classes.className],
                    sectionName = row[Classes.sectionName] ?: "",
                    academicYearId = row[AcademicYears.id].toString(),
                    academicYearName = row[AcademicYears.year] ?: "",
                    academicYearStartDate = row[AcademicYears.startDate].toString(),
                    academicYearEndDate = row[AcademicYears.endDate].toString(),
                    isActiveYear = row[AcademicYears.isActive]
                )
            }

        // Get subjects for each class the student is/was enrolled in
        val subjectsData = mutableListOf<StudentSubjectDto>()
        academicAssignments.forEach { assignment ->
            val subjects = ClassSubjects
                .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
                .selectAll()
                .where {
                    (ClassSubjects.classId eq UUID.fromString(assignment.classId)) and
                            (ClassSubjects.academicYearId eq UUID.fromString(assignment.academicYearId))
                }
                .map { row ->
                    StudentSubjectDto(
                        subjectId = row[Subjects.id].toString(),
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code]?:"",
                        classId = assignment.classId,
                        className = assignment.className,
                        sectionName = assignment.sectionName,
                        academicYearId = assignment.academicYearId,
                        academicYearName = assignment.academicYearName
                    )
                }
            subjectsData.addAll(subjects)
        }

        // Find current academic year
        val currentAcademicYear = academicAssignments.find { it.isActiveYear }

        // Get upcoming exams for current academic year
        val today = LocalDate.now()
        val upcomingExams = if (currentAcademicYear != null) {
            Exams
                .join(Subjects, JoinType.INNER, Exams.subjectId, Subjects.id)
                .join(Classes, JoinType.INNER, Exams.classId, Classes.id)
                .join(ExamSchedules, JoinType.LEFT, Exams.id, ExamSchedules.examId)
                .selectAll()
                .where {
                    (Exams.classId eq UUID.fromString(currentAcademicYear.classId)) and
                            (Exams.academicYearId eq UUID.fromString(currentAcademicYear.academicYearId)) and
                            (Exams.date greaterEq today)
                }
                .orderBy(Exams.date to SortOrder.ASC)
                .map { row ->
                    StudentUpcomingExamDto(
                        examId = row[Exams.id].toString(),
                        examName = row[Exams.name],
                        subjectName = row[Subjects.name],
                        subjectCode = row[Subjects.code]?:"",
                        examDate = row[Exams.date].toString(),
                        maxMarks = row[Exams.maxMarks],
                        startTime = row.getOrNull(ExamSchedules.startTime)?.toString(),
                        endTime = row.getOrNull(ExamSchedules.endTime)?.toString(),
                        daysUntilExam = ChronoUnit.DAYS.between(today, row[Exams.date]).toInt()
                    )
                }
        } else emptyList()

        // Get class teachers for current class
        val classTeachers = if (currentAcademicYear != null) {
            StaffClassAssignments
                .join(Users, JoinType.INNER, StaffClassAssignments.staffId, Users.id)
                .selectAll()
                .where {
                    (StaffClassAssignments.classId eq UUID.fromString(currentAcademicYear.classId)) and
                            (StaffClassAssignments.academicYearId eq UUID.fromString(currentAcademicYear.academicYearId))
                }
                .map { row ->
                    StudentClassTeacherDto(
                        teacherId = row[Users.id].toString(),
                        teacherName = "${row[Users.firstName]} ${row[Users.lastName]}",
                        email = row[Users.email],
                        role = row[StaffClassAssignments.role].toString()
                    )
                }
        } else emptyList()

        StudentBasicDataDto(
            // Basic student information
            studentId = studentInfo[Users.id].toString(),
            firstName = studentInfo[Users.firstName],
            lastName = studentInfo[Users.lastName],
            email = studentInfo[Users.email],
            mobileNumber = studentInfo[Users.mobileNumber] ?: "",
            createdAt = studentInfo[Users.createdAt].toString(),
            updatedAt = studentInfo[Users.updatedAt].toString(),

            // Academic assignments and current status
            academicAssignments = academicAssignments,
            currentAcademicYear = currentAcademicYear,
            subjects = subjectsData,

            // Upcoming information
            upcomingExams = upcomingExams,
            classTeachers = classTeachers
        )
    }
}