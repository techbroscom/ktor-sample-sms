package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.ChatService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.chatRoutes(chatService: ChatService) {
    route("/api/v1/chat") {

        // Chat Room routes
        route("/rooms") {

            // Create chat room
            post {
                val request = call.receive<CreateChatRoomRequest>()
                val createdBy = call.request.headers["User-ID"]
                    ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                val room = chatService.createChatRoom(request, createdBy)
                call.respond(HttpStatusCode.Created, ApiResponse(
                    success = true,
                    data = room,
                    message = "Chat room created successfully"
                ))
            }

            // Get user's chat rooms
            get("/user/{userId}") {
                val userId = call.parameters["userId"]
                    ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

                val rooms = chatService.getChatRoomsByUserId(userId)
                call.respond(ApiResponse(
                    success = true,
                    data = rooms
                ))
            }

            // Get specific chat room
            get("/{roomId}") {
                val roomId = call.parameters["roomId"]
                    ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                val room = chatService.getChatRoomById(roomId)
                call.respond(ApiResponse(
                    success = true,
                    data = room
                ))
            }

            // Update chat room
            put("/{roomId}") {
                val roomId = call.parameters["roomId"]
                    ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                val request = call.receive<UpdateChatRoomRequest>()
                val room = chatService.updateChatRoom(roomId, request)
                call.respond(ApiResponse(
                    success = true,
                    data = room,
                    message = "Chat room updated successfully"
                ))
            }

            // Delete chat room
            delete("/{roomId}") {
                val roomId = call.parameters["roomId"]
                    ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                chatService.deleteChatRoom(roomId)
                call.respond(ApiResponse<Unit>(
                    success = true,
                    message = "Chat room deleted successfully"
                ))
            }

            // Room member management
            route("/{roomId}/members") {

                // Get room members
                get {
                    val roomId = call.parameters["roomId"]
                        ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                    val members = chatService.getRoomMembers(roomId)
                    call.respond(ApiResponse(
                        success = true,
                        data = members
                    ))
                }

                // Add members to room
                post {
                    val roomId = call.parameters["roomId"]
                        ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                    val requesterId = call.request.headers["User-ID"]
                        ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                    val request = call.receive<AddMemberRequest>()
                    val members = chatService.addMembersToRoom(roomId, request, requesterId)
                    call.respond(ApiResponse(
                        success = true,
                        data = members,
                        message = "Members added successfully"
                    ))
                }

                // Update member role
                put("/{userId}/role") {
                    val roomId = call.parameters["roomId"]
                        ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                    val userId = call.parameters["userId"]
                        ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

                    val requesterId = call.request.headers["User-ID"]
                        ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                    val request = call.receive<UpdateMemberRoleRequest>()
                    val member = chatService.updateMemberRole(roomId, userId, request, requesterId)
                    call.respond(ApiResponse(
                        success = true,
                        data = member,
                        message = "Member role updated successfully"
                    ))
                }

                // Remove member from room
                delete("/{userId}") {
                    val roomId = call.parameters["roomId"]
                        ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                    val userId = call.parameters["userId"]
                        ?: throw ApiException("User ID is required", HttpStatusCode.BadRequest)

                    val requesterId = call.request.headers["User-ID"]
                        ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                    chatService.removeMemberFromRoom(roomId, userId, requesterId)
                    call.respond(ApiResponse<Unit>(
                        success = true,
                        message = "Member removed successfully"
                    ))
                }
            }
        }

        // Messages routes
        route("/messages") {

            // Send message
            post {
                val senderId = call.request.headers["User-ID"]
                    ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                val request = call.receive<SendMessageRequest>()
                val message = chatService.sendMessage(request, senderId)
                call.respond(HttpStatusCode.Created, ApiResponse(
                    success = true,
                    data = message,
                    message = "Message sent successfully"
                ))
            }

            // Get room messages
            get("/room/{roomId}") {
                val roomId = call.parameters["roomId"]
                    ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                val userId = call.request.headers["User-ID"]
                    ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                val offset = call.request.queryParameters["offset"]?.toLongOrNull() ?: 0

                val messages = chatService.getRoomMessages(roomId, userId, limit, offset)
                call.respond(ApiResponse(
                    success = true,
                    data = messages
                ))
            }

            // Get specific message
            get("/{messageId}") {
                val messageId = call.parameters["messageId"]
                    ?: throw ApiException("Message ID is required", HttpStatusCode.BadRequest)

                val message = chatService.getMessageById(messageId)
                call.respond(ApiResponse(
                    success = true,
                    data = message
                ))
            }

            // Edit message
            put("/{messageId}") {
                val messageId = call.parameters["messageId"]
                    ?: throw ApiException("Message ID is required", HttpStatusCode.BadRequest)

                val userId = call.request.headers["User-ID"]
                    ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                val request = call.receive<EditMessageRequest>()
                val message = chatService.editMessage(messageId, request, userId)
                call.respond(ApiResponse(
                    success = true,
                    data = message,
                    message = "Message edited successfully"
                ))
            }

            // Delete message
            delete("/{messageId}") {
                val messageId = call.parameters["messageId"]
                    ?: throw ApiException("Message ID is required", HttpStatusCode.BadRequest)

                val userId = call.request.headers["User-ID"]
                    ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                chatService.deleteMessage(messageId, userId)
                call.respond(ApiResponse<Unit>(
                    success = true,
                    message = "Message deleted successfully"
                ))
            }

            // Mark messages as read
            post("/room/{roomId}/read") {
                val roomId = call.parameters["roomId"]
                    ?: throw ApiException("Room ID is required", HttpStatusCode.BadRequest)

                val userId = call.request.headers["User-ID"]
                    ?: throw ApiException("User ID header is required", HttpStatusCode.BadRequest)

                chatService.markMessagesAsRead(roomId, userId)
                call.respond(ApiResponse<Unit>(
                    success = true,
                    message = "Messages marked as read"
                ))
            }
        }
    }
}