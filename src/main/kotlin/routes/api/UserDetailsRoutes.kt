package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateUserDetailsRequest
import com.example.models.dto.UpdateUserDetailsRequest
import com.example.models.responses.ApiResponse
import com.example.services.UserDetailsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userDetailsRoutes(userDetailsService: UserDetailsService) {
    route("/api/v1/user-details") {

        // Create user details
        post {
            val request = call.receive<CreateUserDetailsRequest>()
            val userDetails = userDetailsService.createUserDetails(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = userDetails,
                message = "User details created successfully"
            ))
        }

        // Get user details by user ID (returns null if not found)
        get("/user/{userId}") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val userDetails = userDetailsService.getUserDetailsByUserId(userId)

            if (userDetails == null) {
                call.respond(HttpStatusCode.NotFound, ApiResponse(
                    success = false,
                    data = null,
                    message = "User details not found. Please create user details first."
                ))
            } else {
                call.respond(ApiResponse(
                    success = true,
                    data = userDetails
                ))
            }
        }

        // Create or update user details (upsert) - recommended endpoint
        put("/user/{userId}/upsert") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateUserDetailsRequest>()
            val userDetails = userDetailsService.createOrUpdateUserDetails(userId, request)
            call.respond(ApiResponse(
                success = true,
                data = userDetails,
                message = "User details saved successfully"
            ))
        }

        // Update user details by user ID
        put("/user/{userId}") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateUserDetailsRequest>()
            val userDetails = userDetailsService.updateUserDetails(userId, request)
            call.respond(ApiResponse(
                success = true,
                data = userDetails,
                message = "User details updated successfully"
            ))
        }

        // Delete user details by user ID
        delete("/user/{userId}") {
            val userId = call.parameters["userId"]
                ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

            userDetailsService.deleteUserDetails(userId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "User details deleted successfully"
            ))
        }
    }
}
