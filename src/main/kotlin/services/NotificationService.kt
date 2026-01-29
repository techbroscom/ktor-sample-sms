package com.example.services

import com.example.database.tables.UserRole
import com.example.models.BroadcastNotificationRequest
import com.example.models.PersonalNotificationRequest
import com.example.repositories.UserRepository

class NotificationService(
    private val fcmService: FCMService,
    private val userRepository: UserRepository
) {

    // Send fee due reminder to specific student
    suspend fun sendFeeReminderNotification(studentId: String, amount: Double, dueDate: String) {
        val notification = PersonalNotificationRequest(
            title = "Fee Due Reminder",
            body = "Your fee of ₹$amount is due on $dueDate. Please pay to avoid late charges.",
            userId = studentId,
            data = mapOf(
                "type" to "fee_reminder",
                "amount" to amount.toString(),
                "dueDate" to dueDate
            )
        )

        fcmService.sendPersonalNotification(notification)
    }

    // Send exam result notification
    suspend fun sendExamResultNotification(studentId: String, examName: String, marks: Int, totalMarks: Int) {
        val notification = PersonalNotificationRequest(
            title = "Exam Result Available",
            body = "Your result for $examName is now available. You scored $marks/$totalMarks",
            userId = studentId,
            data = mapOf(
                "type" to "exam_result",
                "examName" to examName,
                "marks" to marks.toString(),
                "totalMarks" to totalMarks.toString()
            )
        )

        fcmService.sendPersonalNotification(notification)
    }

    // Send attendance alert
    suspend fun sendAttendanceAlert(studentId: String, attendancePercentage: Float) {
        val notification = PersonalNotificationRequest(
            title = "Low Attendance Alert",
            body = "Your attendance is ${attendancePercentage}%. Minimum required is 75%.",
            userId = studentId,
            data = mapOf(
                "type" to "attendance_alert",
                "percentage" to attendancePercentage.toString()
            )
        )

        fcmService.sendPersonalNotification(notification)
    }

    // Send school announcement to all students
    suspend fun sendSchoolAnnouncement(schoolId: Int, title: String, message: String) {
        println("Sending school announcement notification")
        val notification = BroadcastNotificationRequest(
            title = title,
            body = message,
            schoolId = schoolId,
            targetRole = UserRole.STUDENT,
            data = mapOf(
                "type" to "school_announcement"
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send news/updates to entire school
    suspend fun sendSchoolNews(schoolId: Int, title: String, message: String) {
        val notification = BroadcastNotificationRequest(
            title = title,
            body = message,
            schoolId = schoolId,
            targetRole = null, // Send to all roles
            data = mapOf(
                "type" to "school_news"
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send holiday notification
    suspend fun sendHolidayNotification(schoolId: Int, holidayName: String, date: String) {
        val notification = BroadcastNotificationRequest(
            title = "Holiday Notification",
            body = "$holidayName on $date. School will remain closed.",
            schoolId = schoolId,
            targetRole = null,
            data = mapOf(
                "type" to "holiday",
                "holidayName" to holidayName,
                "date" to date
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send assignment notification to students
    suspend fun sendAssignmentNotification(schoolId: Int, className: String, subject: String, dueDate: String) {
        val notification = BroadcastNotificationRequest(
            title = "New Assignment",
            body = "New $subject assignment for $className. Due date: $dueDate",
            schoolId = schoolId,
            targetRole = UserRole.STUDENT,
            data = mapOf(
                "type" to "assignment",
                "className" to className,
                "subject" to subject,
                "dueDate" to dueDate
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send exam schedule notification
    suspend fun sendExamScheduleNotification(schoolId: Int, examName: String, startDate: String) {
        val notification = BroadcastNotificationRequest(
            title = "Exam Schedule Released",
            body = "$examName schedule has been released. Exam starts from $startDate",
            schoolId = schoolId,
            targetRole = UserRole.STUDENT,
            data = mapOf(
                "type" to "exam_schedule",
                "examName" to examName,
                "startDate" to startDate
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send teacher notification
    suspend fun sendTeacherNotification(schoolId: Int, title: String, message: String) {
        val notification = BroadcastNotificationRequest(
            title = title,
            body = message,
            schoolId = schoolId,
            targetRole = UserRole.STUDENT,
            data = mapOf(
                "type" to "teacher_notification"
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send parent notification
    suspend fun sendParentNotification(schoolId: Int, title: String, message: String) {
        val notification = BroadcastNotificationRequest(
            title = title,
            body = message,
            schoolId = schoolId,
            targetRole = UserRole.STUDENT,
            data = mapOf(
                "type" to "parent_notification"
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send complaint notification to admins (when a new complaint is submitted)
    suspend fun sendComplaintNotification(title: String, message: String) {
        println("Sending complaint notification to admins")
        val notification = BroadcastNotificationRequest(
            title = title,
            body = message,
            schoolId = 1,
            targetRole = UserRole.ADMIN,
            data = mapOf(
                "type" to "complaint_notification"
            )
        )

        fcmService.sendBroadcastNotification(notification)
    }

    // Send complaint status update notification to the complaint author
    suspend fun sendComplaintStatusUpdateNotification(userId: String, complaintTitle: String, newStatus: String) {
        println("Sending complaint status update notification to user: $userId")
        val notification = PersonalNotificationRequest(
            title = "Complaint Status Updated",
            body = "Your complaint \"$complaintTitle\" has been updated to: $newStatus",
            userId = userId,
            data = mapOf(
                "type" to "complaint_status_update",
                "status" to newStatus
            )
        )

        fcmService.sendPersonalNotification(notification)
    }
}