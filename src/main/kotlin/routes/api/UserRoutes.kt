package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(userService: UserService) {
    route("/api/v1/users") {

        // Authentication
        post("/login") {
            val request = call.receive<UserLoginRequest>()
            println(request.toString())
            val response = userService.authenticateUser(request)
            println(response.toString())
            call.respond(ApiResponse(
                success = true,
                data = response,
                message = "Login successful"
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

        // Get users by role
        get("/role/{role}") {
            val role = call.parameters["role"]
                ?: throw ApiException("Role parameter is required", HttpStatusCode.BadRequest)

            val users = userService.getUsersByRole(role)
            call.respond(ApiResponse(
                success = true,
                data = users
            ))
        }

        // Get user by email
        get("/email/{email}") {
            val email = call.parameters["email"]
                ?: throw ApiException("Email parameter is required", HttpStatusCode.BadRequest)

            val user = userService.getUserByEmail(email)
            call.respond(ApiResponse(
                success = true,
                data = user
            ))
        }

        // Get user by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val user = userService.getUserById(id)
            call.respond(ApiResponse(
                success = true,
                data = user
            ))
        }

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

        // Update user
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateUserRequest>()
            val user = userService.updateUser(id, request)
            call.respond(ApiResponse(
                success = true,
                data = user,
                message = "User updated successfully"
            ))
        }

        // Change password
        put("/{id}/password") {
            val id = call.parameters["id"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<ChangePasswordRequest>()
            val user = userService.changePassword(id, request)
            call.respond(ApiResponse(
                success = true,
                data = user,
                message = "Password changed successfully"
            ))
        }

        // Delete user
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            userService.deleteUser(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "User deleted successfully"
            ))
        }
    }
}