package com.example.routes.api

import com.example.models.*
import com.example.services.FCMService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.fcmRoutes(fcmService: FCMService) {

    route("/api/v1/fcm") {

        // Save FCM token
        post("/token") {
            try {
                val userId = call.request.headers["userId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")

                val tokenRequest = call.receive<FCMTokenRequest>()
                val success = fcmService.saveToken(UUID.fromString(userId), tokenRequest)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Token saved successfully"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to save token"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Send personal notification
        post("/notification/personal") {
            try {
                val request = call.receive<PersonalNotificationRequest>()
                val response = fcmService.sendPersonalNotification(request)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    NotificationResponse(false, "Failed to send notification: ${e.message}"))
            }
        }

        // Send broadcast notification
        post("/notification/broadcast") {
            try {
                val request = call.receive<BroadcastNotificationRequest>()
                val response = fcmService.sendBroadcastNotification(request)

                if (response.success) {
                    call.respond(HttpStatusCode.OK, response)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, response)
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError,
                    NotificationResponse(false, "Failed to send broadcast notification: ${e.message}"))
            }
        }

        // Subscribe to topic
        post("/topic/subscribe") {
            try {
                val userId = call.request.headers["userId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")

                val topic = call.receive<Map<String, String>>()["topic"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Topic required")

                val success = fcmService.subscribeToTopic(UUID.fromString(userId), topic)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Subscribed to topic successfully"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to subscribe"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }

        // Unsubscribe from topic
        post("/topic/unsubscribe") {
            try {
                val userId = call.request.headers["userId"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "User ID required")

                val topic = call.receive<Map<String, String>>()["topic"]
                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Topic required")

                val success = fcmService.unsubscribeFromTopic(UUID.fromString(userId), topic)

                if (success) {
                    call.respond(HttpStatusCode.OK, mapOf("message" to "Unsubscribed from topic successfully"))
                } else {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to unsubscribe"))
                }

            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to e.message))
            }
        }
    }
}