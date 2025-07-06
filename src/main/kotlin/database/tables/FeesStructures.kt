package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.math.BigDecimal

object FeesStructures : Table("fees_structures") {
    val id = uuid("id").autoGenerate()
    val classId = uuid("class_id").references(Classes.id)
    val academicYearId = uuid("academic_year_id").references(AcademicYears.id)
    val name = varchar("name", 100)
    val amount = decimal("amount", 10, 2)
    val isMandatory = bool("is_mandatory").default(true)
    val createdAt = datetime("created_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)
    val updatedAt = datetime("updated_at").defaultExpression(org.jetbrains.exposed.sql.javatime.CurrentDateTime)

    override val primaryKey = PrimaryKey(id)

    // Unique constraint to prevent duplicate fee structures for same class, academic year, and name
    init {
        uniqueIndex(classId, academicYearId, name)
    }
}