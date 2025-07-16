package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

object StudentTransportAssignments : Table("student_transport_assignments") {
    val id = uuid("id")
    val studentId = uuid("student_id") references Users.id
    val academicYearId = uuid("academic_year_id") references AcademicYears.id
    val routeId = uuid("route_id") references TransportRoutes.id
    val stopId = uuid("stop_id") references TransportStops.id
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(studentId, academicYearId)
    }
}