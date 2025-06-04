package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object ExamResults : Table("exam_results") {
    val id = uuid("id").autoGenerate()
    val examId = uuid("exam_id").references(Exams.id)
    val studentId = uuid("student_id").references(Users.id)
    val marksObtained = integer("marks_obtained")
    val grade = varchar("grade", 10).nullable()

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate results for same exam-student combination
    init {
        uniqueIndex(examId, studentId)
    }
}