package com.example.plugins

import com.example.config.DatabaseConfig
import com.example.database.tables.AcademicYears
import com.example.database.tables.ClassSubjects
import com.example.database.tables.Classes
import com.example.database.tables.Complaints
import com.example.database.tables.ExamSchedules
import com.example.database.tables.Exams
import com.example.database.tables.Holidays
import com.example.database.tables.OtpCodes
import com.example.database.tables.Posts
import com.example.database.tables.Rules
import com.example.database.tables.SchoolConfig
import com.example.database.tables.StaffClassAssignments
import com.example.database.tables.StaffSubjectAssignments
import com.example.database.tables.StudentAssignments
import com.example.database.tables.Subjects
import com.example.database.tables.Users
import io.ktor.server.application.*
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun Application.configureDatabases() {
    val database = DatabaseConfig.init()

    // Create tables
    transaction(database) {
        SchemaUtils.create(Users, Holidays, Posts, Complaints, OtpCodes, SchoolConfig, Rules, AcademicYears, Subjects, Classes, ClassSubjects, StudentAssignments, StaffClassAssignments, StaffSubjectAssignments, Exams, ExamSchedules)
    }
}