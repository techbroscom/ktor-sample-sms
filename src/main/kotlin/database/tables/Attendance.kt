package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Attendance : Table("attendance") {
    val id = uuid("id").autoGenerate()
    val studentId = uuid("student_id").references(Users.id)
    val classId = uuid("class_id").references(Classes.id)
    val date = date("date")
    val status = enumeration("status", AttendanceStatus::class)

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate attendance records for same student, class, and date
    init {
        uniqueIndex(studentId, classId, date)
    }
}

enum class AttendanceStatus {
    PRESENT,
    ABSENT,
    LATE
}