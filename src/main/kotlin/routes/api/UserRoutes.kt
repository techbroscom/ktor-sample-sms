package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.OtpService
import com.example.services.UserService
import com.example.tenant.TenantContextHolder
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

fun Route.userRoutes(userService: UserService, otpService: OtpService) {
    route("/api/v1/users") {

        get("/tenant-info") {
            val tenant = TenantContextHolder.getTenant()
            call.respond(mapOf(
                "tenant" to tenant?.name,
                "schema" to tenant?.schemaName
            ))
        }

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

        //with FCM
        post("/login-fcm") {
            val request = call.receive<UserLoginFCMRequest>()
            println(request.toString())
            // ADD THIS DEBUG LOG
            val currentTenant = TenantContextHolder.getTenant()
            println("[Debug] Current Tenant Schema: ${currentTenant?.schemaName}")

            val response = userService.authenticateUserWithFCM(request)
            println(response.toString())
            call.respond(ApiResponse(
                success = true,
                data = response,
                message = "Login successful"
            ))
        }

        // NEW: Send OTP to email
        post("/login/send-otp") {
            println("Received request to send OTP")
            val request = call.receive<SendOtpRequest>()
            // Basic validation
            if (request.email.isBlank()) {
                println("Validating email1: ${request.email}")
                throw ApiException("Email cannot be empty", HttpStatusCode.BadRequest)
            }

            println("Validating email2: ${request.email}")
            val message = otpService.sendOtp(request.email)
            println("OTP sent successfully")
            call.respond(ApiResponse(
                success = true,
                data = SendOtpResponse(message, request.email),
                message = "OTP sent successfully"
            ))
        }

        // NEW: Verify OTP and login
        post("/login/verify-otp") {
            val request = call.receive<VerifyOtpRequest>()

            // Basic validation
            if (request.email.isBlank()) {
                throw ApiException("Email cannot be empty", HttpStatusCode.BadRequest)
            }
            if (request.otpCode.isBlank()) {
                throw ApiException("OTP code cannot be empty", HttpStatusCode.BadRequest)
            }
            if (request.otpCode.length != 6) {
                throw ApiException("OTP code must be 6 digits", HttpStatusCode.BadRequest)
            }

            val response = otpService.verifyOtpAndLogin(request.email, request.otpCode)
            call.respond(ApiResponse(
                success = true,
                data = response,
                message = "Login successful"
            ))
        }

        // Password Reset: Send OTP to user's email
        post("/forgot-password/send-otp") {
            val request = call.receive<ForgotPasswordSendOtpRequest>()

            val response = userService.sendPasswordResetOtp(request.mobileNumber)
            call.respond(ApiResponse(
                success = true,
                data = response,
                message = "Password reset code sent successfully"
            ))
        }

        // Password Reset: Verify OTP and reset password
        post("/forgot-password/reset") {
            val request = call.receive<ForgotPasswordResetRequest>()

            val user = userService.resetPasswordWithOtp(
                request.mobileNumber,
                request.otpCode,
                request.newPassword
            )
            call.respond(ApiResponse(
                success = true,
                data = user,
                message = "Password reset successfully"
            ))
        }

        // Get users with filters (role, class, search) - with pagination
        get("/filter") {
            val role = call.request.queryParameters["role"]
            val classId = call.request.queryParameters["classId"]
            val search = call.request.queryParameters["search"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20

            val paginatedUsers = userService.getUsersWithFilters(role, classId, search, page, pageSize)
            call.respond(paginatedUsers)
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

        // Get user by ADMIN
        get("/ADMIN") {
            val user = userService.getAdminUsers()
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

        // Upload user photo
        post("/{id}/upload-photo") {
            try {
                val userId = call.parameters["id"]
                    ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

                val tenantId = call.request.headers["X-Tenant"] ?: "default"
                val multipartData = call.receiveMultipart()

                var imageBytes: ByteArray? = null
                var imageFileName: String? = null

                multipartData.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            if (part.name == "photo" || part.name == "image") {
                                imageFileName = part.originalFileName ?: "photo.jpg"
                                imageBytes = withContext(Dispatchers.IO) {
                                    try {
                                        val stream = part.streamProvider()
                                        val buffer = ByteArrayOutputStream()
                                        stream.use { inputStream ->
                                            inputStream.copyTo(buffer)
                                        }
                                        buffer.toByteArray()
                                    } catch (e: Exception) {
                                        println("Error reading image stream: ${e.message}")
                                        ByteArray(0)
                                    }
                                }
                            }
                            part.dispose()
                        }
                        else -> {
                            part.dispose()
                        }
                    }
                }

                if (imageBytes == null || imageBytes!!.isEmpty()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Unit>(
                        success = false,
                        message = "Photo file is required"
                    ))
                    return@post
                }

                // Upload photo
                val updatedUser = userService.uploadUserPhoto(
                    userId = userId,
                    tenantId = tenantId,
                    inputStream = imageBytes!!.inputStream(),
                    originalFileName = imageFileName!!
                )

                call.respond(HttpStatusCode.OK, ApiResponse(
                    success = true,
                    data = updatedUser,
                    message = "Photo uploaded successfully"
                ))

            } catch (e: ApiException) {
                throw e
            } catch (e: Exception) {
                println("ERROR uploading user photo: ${e.message}")
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Unit>(
                        success = false,
                        message = "Failed to upload photo: ${e.message}"
                    )
                )
            }
        }
    }
}