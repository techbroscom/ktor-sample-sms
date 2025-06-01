package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.AcademicYearService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.academicYearRoutes(academicYearService: AcademicYearService) {
    route("/api/v1/academic-years") {

        // Get all academic years
        get {
            val academicYears = academicYearService.getAllAcademicYears()
            call.respond(ApiResponse(
                success = true,
                data = academicYears
            ))
        }

        // Get active academic year
        get("/active") {
            val academicYear = academicYearService.getActiveAcademicYear()
            call.respond(ApiResponse(
                success = true,
                data = academicYear
            ))
        }

        // Get current academic year (based on current date)
        get("/current") {
            val academicYear = academicYearService.getCurrentAcademicYear()
            call.respond(ApiResponse(
                success = true,
                data = academicYear
            ))
        }

        // Get academic years by date range
        get("/range") {
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("startDate parameter is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("endDate parameter is required", HttpStatusCode.BadRequest)

            val academicYears = academicYearService.getAcademicYearsByDateRange(startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = academicYears
            ))
        }

        // Get academic year by year string
        get("/year/{year}") {
            val year = call.parameters["year"]
                ?: throw ApiException("Year parameter is required", HttpStatusCode.BadRequest)

            val academicYear = academicYearService.getAcademicYearByYear(year)
            call.respond(ApiResponse(
                success = true,
                data = academicYear
            ))
        }

        // Get academic year by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Academic year ID is required", HttpStatusCode.BadRequest)

            val academicYear = academicYearService.getAcademicYearById(id)
            call.respond(ApiResponse(
                success = true,
                data = academicYear
            ))
        }

        // Create academic year
        post {
            val request = call.receive<CreateAcademicYearRequest>()
            println(request)
            val academicYear = academicYearService.createAcademicYear(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = academicYear,
                message = "Academic year created successfully"
            ))
        }

        // Update academic year
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Academic year ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateAcademicYearRequest>()
            val academicYear = academicYearService.updateAcademicYear(id, request)
            call.respond(ApiResponse(
                success = true,
                data = academicYear,
                message = "Academic year updated successfully"
            ))
        }

        // Set active status
        put("/{id}/active") {
            val id = call.parameters["id"]
                ?: throw ApiException("Academic year ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<SetActiveAcademicYearRequest>()
            val academicYear = academicYearService.setActiveAcademicYear(id, request)
            call.respond(ApiResponse(
                success = true,
                data = academicYear,
                message = if (request.isActive) "Academic year activated successfully" else "Academic year deactivated successfully"
            ))
        }

        // Delete academic year
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Academic year ID is required", HttpStatusCode.BadRequest)

            academicYearService.deleteAcademicYear(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Academic year deleted successfully"
            ))
        }
    }
}