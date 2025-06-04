package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object StaffSubjectAssignments : Table("staff_subject_assignments") {
    val id = uuid("id").autoGenerate()
    val staffId = uuid("staff_id").references(Users.id)
    val classSubjectId = uuid("class_subject_id").references(ClassSubjects.id)
    val classId = uuid("class_id").references(Classes.id)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate staff-subject-class-academic year combinations
    init {
        uniqueIndex(staffId, classSubjectId, classId, academicYearId)
    }
}