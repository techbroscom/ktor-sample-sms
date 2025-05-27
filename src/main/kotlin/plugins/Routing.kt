package com.example.plugins

import com.example.repositories.HolidayRepository
import com.example.repositories.UserRepository
import com.example.routes.api.holidayRoutes
import com.example.routes.api.userRoutes
import com.example.services.HolidayService
import com.example.services.UserService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // Initialize dependencies
    val userRepository = UserRepository()
    val userService = UserService(userRepository)

    val holidayRepository = HolidayRepository()
    val holidayService = HolidayService(holidayRepository)

    routing {
        get("/") {
            call.respondText("School Management API Server")
        }

        get("/health") {
            call.respondText("OK")
        }

        // API routes
        userRoutes(userService)
        holidayRoutes(holidayService)
    }
}