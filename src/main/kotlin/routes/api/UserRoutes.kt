package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateUserRequest
import com.example.models.dto.UpdateUserRequest
import com.example.models.responses.ApiResponse
import com.example.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/api/v1/users") {

        // Create user
        post {
            val request = call.receive<CreateUserRequest>()
            val user = userService.createUser(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = user,
                message = "User created successfully"
            ))
        }

        // Get user by ID
        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid user ID", HttpStatusCode.BadRequest)

            val user = userService.getUserById(id)
            call.respond(ApiResponse(
                success = true,
                data = user
            ))
        }

        // Update user
        put("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid user ID", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateUserRequest>()
            val user = userService.updateUser(id, request)
            call.respond(ApiResponse(
                success = true,
                data = user,
                message = "User updated successfully"
            ))
        }

        // Delete user
        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
                ?: throw ApiException("Invalid user ID", HttpStatusCode.BadRequest)

            userService.deleteUser(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "User deleted successfully"
            ))
        }

        // Get all users
        get {
            val users = userService.getAllUsers()
            call.respond(ApiResponse(
                success = true,
                data = users
            ))
        }
    }
}