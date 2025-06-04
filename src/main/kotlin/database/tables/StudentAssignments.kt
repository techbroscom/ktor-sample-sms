package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object StudentAssignments : Table("student_assignments") {
    val id = uuid("id").autoGenerate()
    val studentId = uuid("student_id").references(Users.id)
    val classId = uuid("class_id").references(Classes.id)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate student-class-academic year combinations
    init {
        uniqueIndex(studentId, classId, academicYearId)
    }
}