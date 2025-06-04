package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object StaffClassAssignments : Table("staff_class_assignments") {
    val id = uuid("id").autoGenerate()
    val staffId = uuid("staff_id").references(Users.id)
    val classId = uuid("class_id").references(Classes.id)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)
    val role = enumerationByName("role", 50, StaffClassRole::class).nullable()

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate staff-class-academic year combinations
    init {
        uniqueIndex(staffId, classId, academicYearId)
    }
}

enum class StaffClassRole {
    CLASS_TEACHER,
    COORDINATOR,
    ASSISTANT
}