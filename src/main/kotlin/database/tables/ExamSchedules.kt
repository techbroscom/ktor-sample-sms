package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object ExamSchedules : Table("exam_schedules") {
    val id = uuid("id").autoGenerate()
    val examId = uuid("exam_id").references(Exams.id)
    val classId = uuid("class_id").references(Classes.id)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time")

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate exam schedules for same exam-class combination
    init {
        uniqueIndex(examId, classId)
    }
}