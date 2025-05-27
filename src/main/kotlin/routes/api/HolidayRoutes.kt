package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateHolidayRequest
import com.example.models.dto.UpdateHolidayRequest
import com.example.models.responses.ApiResponse
import com.example.services.HolidayService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.holidayRoutes(holidayService: HolidayService) {
    route("/api/v1/holidays") {

        // Get all holidays
        get {
            val holidays = holidayService.getAllHolidays()
            call.respond(ApiResponse(
                success = true,
                data = holidays
            ))
        }

        // Get public holidays only
        get("/public") {
            val holidays = holidayService.getPublicHolidays()
            call.respond(ApiResponse(
                success = true,
                data = holidays
            ))
        }

        // Get holidays by date range
        get("/range") {
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("startDate parameter is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("endDate parameter is required", HttpStatusCode.BadRequest)

            val holidays = holidayService.getHolidaysByDateRange(startDate, endDate)
            call.respond(ApiResponse(
                success = true,
                data = holidays
            ))
        }

        // Get holiday by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid holiday ID", HttpStatusCode.BadRequest)

            val holiday = holidayService.getHolidayById(id)
            call.respond(ApiResponse(
                success = true,
                data = holiday
            ))
        }

        // Create holiday
        post {
            val request = call.receive<CreateHolidayRequest>()
            val holiday = holidayService.createHoliday(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = holiday,
                message = "Holiday created successfully"
            ))
        }

        // Update holiday
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid holiday ID", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateHolidayRequest>()
            val holiday = holidayService.updateHoliday(id, request)
            call.respond(ApiResponse(
                success = true,
                data = holiday,
                message = "Holiday updated successfully"
            ))
        }

        // Delete holiday
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid holiday ID", HttpStatusCode.BadRequest)

            holidayService.deleteHoliday(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Holiday deleted successfully"
            ))
        }
    }
}