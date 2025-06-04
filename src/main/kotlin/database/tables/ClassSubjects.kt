package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object ClassSubjects : Table("class_subjects") {
    val id = uuid("id").autoGenerate()
    val classId = uuid("class_id").references(Classes.id)
    val subjectId = uuid("subject_id").references(Subjects.id)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate class-subject-academic year combinations
    init {
        uniqueIndex(classId, subjectId, academicYearId)
    }
}