package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.responses.ApiResponse
import com.example.services.DashboardService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.dashboardRoutes(dashboardService: DashboardService) {
    route("/api/v1/dashboard") {

        // Get overall dashboard overview
        get("/overview") {
            val overview = dashboardService.getDashboardOverview()
            call.respond(ApiResponse(
                success = true,
                data = overview,
                message = "Dashboard overview retrieved successfully"
            ))
        }

        // Get student statistics
        get("/students") {
            val studentStats = dashboardService.getStudentStatistics()
            call.respond(ApiResponse(
                success = true,
                data = studentStats,
                message = "Student statistics retrieved successfully"
            ))
        }

        // Get student statistics
        get("/students/complete/{id}") {
            println("Student ID: ${call.parameters["id"]}")
            val id = call.parameters["id"]
                ?: throw ApiException("Invalid Student ID", HttpStatusCode.BadRequest)
            val studentStats = dashboardService.getStudentStatistics(id)
            call.respond(ApiResponse(
                success = true,
                data = studentStats,
                message = "Student statistics retrieved successfully"
            ))
        }

        // Get student statistics
        get("/students/basic/{id}") {
            println("Student ID: ${call.parameters["id"]}")
            val id = call.parameters["id"]
                ?: throw ApiException("Invalid Student ID", HttpStatusCode.BadRequest)
            val studentStats = dashboardService.getStudentBasicStatistics(id)
            call.respond(ApiResponse(
                success = true,
                data = studentStats,
                message = "Student statistics retrieved successfully"
            ))
        }

        // Get staff statistics
        get("/staff") {
            val staffStats = dashboardService.getStaffStatistics()
            call.respond(ApiResponse(
                success = true,
                data = staffStats,
                message = "Staff statistics retrieved successfully"
            ))
        }

        // Get exam statistics
        get("/exams") {
            val examStats = dashboardService.getExamStatistics()
            call.respond(ApiResponse(
                success = true,
                data = examStats,
                message = "Exam statistics retrieved successfully"
            ))
        }

        // Get attendance statistics
        get("/attendance") {
            val attendanceStats = dashboardService.getAttendanceStatistics()
            call.respond(ApiResponse(
                success = true,
                data = attendanceStats,
                message = "Attendance statistics retrieved successfully"
            ))
        }

        // Get complaint statistics
        get("/complaints") {
            val complaintStats = dashboardService.getComplaintStatistics()
            call.respond(ApiResponse(
                success = true,
                data = complaintStats,
                message = "Complaint statistics retrieved successfully"
            ))
        }

        // Get academic statistics
        get("/academic") {
            val academicStats = dashboardService.getAcademicStatistics()
            call.respond(ApiResponse(
                success = true,
                data = academicStats,
                message = "Academic statistics retrieved successfully"
            ))
        }

        // Get holiday statistics
        get("/holidays") {
            val holidayStats = dashboardService.getHolidayStatistics()
            call.respond(ApiResponse(
                success = true,
                data = holidayStats,
                message = "Holiday statistics retrieved successfully"
            ))
        }

        // Get complete dashboard data
        get("/complete") {
            val completeDashboard = dashboardService.getCompleteDashboard()
            call.respond(ApiResponse(
                success = true,
                data = completeDashboard,
                message = "Complete dashboard data retrieved successfully"
            ))
        }

        // Health check endpoint for dashboard
        get("/health") {
            call.respond(ApiResponse(
                success = true,
                data = mapOf(
                    "status" to "healthy",
                    "timestamp" to System.currentTimeMillis(),
                    "service" to "dashboard"
                ),
                message = "Dashboard service is healthy"
            ))
        }
    }
}