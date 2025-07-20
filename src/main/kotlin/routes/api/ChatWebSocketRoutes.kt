package com.example.routes.api

import com.example.models.websocket.*
import com.example.services.ChatWebSocketService
import com.example.websocket.ChatWebSocketManager
import io.ktor.server.routing.*
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.json.Json
import java.util.*

fun Route.chatWebSocketRoute(
    webSocketManager: ChatWebSocketManager,
    chatWebSocketService: ChatWebSocketService
) {
    val json = Json { ignoreUnknownKeys = true }

    webSocket("/ws/chat") {
        val connectionId = UUID.randomUUID().toString()

        try {
            // Add connection to manager
            webSocketManager.addConnection(connectionId, this)

            // Send connection acknowledgment
            send(Frame.Text(json.encodeToString(WebSocketMessage(
                type = "connection_ack",
                data = json.encodeToString(mapOf("connectionId" to connectionId))
            ))))

            // Handle incoming messages
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        try {
                            val messageText = frame.readText()
                            val webSocketMessage = json.decodeFromString<WebSocketMessage>(messageText)

                            when (webSocketMessage.type) {
                                "user_online" -> {
                                    val userId = json.decodeFromString<String>(webSocketMessage.data)
                                    chatWebSocketService.handleUserOnline(userId, connectionId)
                                }

                                "join_room" -> {
                                    val event = json.decodeFromString<JoinRoomEvent>(webSocketMessage.data)
                                    chatWebSocketService.handleJoinRoom(event, connectionId)
                                }

                                "leave_room" -> {
                                    val event = json.decodeFromString<LeaveRoomEvent>(webSocketMessage.data)
                                    chatWebSocketService.handleLeaveRoom(event, connectionId)
                                }

                                "typing" -> {
                                    val event = json.decodeFromString<TypingEvent>(webSocketMessage.data)
                                    chatWebSocketService.handleTyping(event, connectionId)
                                }

                                "ping" -> {
                                    send(Frame.Text(json.encodeToString(WebSocketMessage(
                                        type = "pong",
                                        data = "{}"
                                    ))))
                                }

                                else -> {
                                    webSocketManager.sendError(connectionId, "Unknown message type: ${webSocketMessage.type}", "UNKNOWN_MESSAGE_TYPE")
                                }
                            }
                        } catch (e: Exception) {
                            webSocketManager.sendError(connectionId, "Failed to process message: ${e.message}", "MESSAGE_PROCESSING_ERROR")
                        }
                    }

                    is Frame.Close -> {
                        break
                    }

                    else -> {
                        // Ignore other frame types
                    }
                }
            }

        } catch (e: ClosedReceiveChannelException) {
            println("WebSocket connection closed: $connectionId")
        } catch (e: Exception) {
            println("WebSocket error for connection $connectionId: ${e.message}")
        } finally {
            webSocketManager.removeConnection(connectionId)
        }
    }
}