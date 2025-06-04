package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object Classes : Table("classes") {
    val id = uuid("id").autoGenerate()
    val className = varchar("class_name", 50)
    val sectionName = varchar("section_name", 50)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)

    override val primaryKey = PrimaryKey(id)
}