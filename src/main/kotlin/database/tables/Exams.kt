package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object Exams : Table("exams") {
    val id = uuid("id").autoGenerate()
    val name = varchar("name", 100)
    val subjectId = uuid("subject_id").references(Subjects.id)
    val classId = uuid("class_id").references(Classes.id)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)
    val maxMarks = integer("max_marks")
    val date = date("date")

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate exam names for same class-subject-academic year combination
    init {
        uniqueIndex(name, classId, subjectId, academicYearId)
    }
}