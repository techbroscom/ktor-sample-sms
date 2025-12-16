package com.example.database.tables

import com.example.database.tables.ExamResults.default
import com.example.database.tables.ExamResults.nullable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object Exams : Table("exams") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val subjectId = uuid("subject_id").references(Subjects.id)
    val classId = uuid("class_id").references(Classes.id)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)
    val maxMarks = integer("max_marks")
    val date = date("date")

    // ✅ NEW
    val resultStatus = enumerationByName(
        "result_status",
        20,
        ResultStatus::class
    ).default(ResultStatus.NOT_STARTED)

    val resultsPublishedAt = datetime("results_published_at").nullable()

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate exam names for same class-subject-academic year combination
    init {
        uniqueIndex(name, classId, subjectId, academicYearId)
    }
}

enum class ResultStatus {
    NOT_STARTED,
    IN_PROGRESS,
    READY,
    PUBLISHED
}