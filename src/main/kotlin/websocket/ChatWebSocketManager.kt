package com.example.websocket

import com.example.models.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet

class ChatWebSocketManager {

    // Store active connections
    private val connections = ConcurrentHashMap<String, WebSocketSession>()

    // Store user to connection mapping
    private val userConnections = ConcurrentHashMap<String, String>()

    // Store room memberships
    private val roomMembers = ConcurrentHashMap<String, CopyOnWriteArraySet<String>>()

    // Store typing users per room
    private val typingUsers = ConcurrentHashMap<String, CopyOnWriteArraySet<String>>()

    // Store online users
    private val onlineUsers = CopyOnWriteArraySet<String>()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun addConnection(connectionId: String, session: WebSocketSession) {
        connections[connectionId] = session
        println("WebSocket connection added: $connectionId")
    }

    suspend fun removeConnection(connectionId: String) {
        connections.remove(connectionId)

        // Find and remove user from online status
        val userId = userConnections.entries.find { it.value == connectionId }?.key
        if (userId != null) {
            userConnections.remove(userId)
            onlineUsers.remove(userId)

            // Remove from all rooms
            roomMembers.forEach { (roomId, members) ->
                if (members.remove(userId)) {
                    broadcastToRoom(roomId, "user_offline", UserOnlineEvent(
                        userId = userId,
                        userName = "", // Will be populated by service
                        isOnline = false,
                        lastSeen = System.currentTimeMillis().toString()
                    ))
                }
            }

            // Remove from typing users
            typingUsers.forEach { (roomId, users) ->
                if (users.remove(userId)) {
                    broadcastToRoom(roomId, "user_stopped_typing", TypingEvent(
                        roomId = roomId,
                        userId = userId,
                        userName = "",
                        isTyping = false
                    ))
                }
            }
        }

        println("WebSocket connection removed: $connectionId")
    }

    suspend fun handleUserOnline(userId: String, userName: String, connectionId: String) {
        userConnections[userId] = connectionId
        onlineUsers.add(userId)

        // Notify all rooms where user is a member
        roomMembers.forEach { (roomId, members) ->
            if (members.contains(userId)) {
                broadcastToRoom(roomId, "user_online", UserOnlineEvent(
                    userId = userId,
                    userName = userName,
                    isOnline = true
                ))
            }
        }
    }

    suspend fun joinRoom(roomId: String, userId: String, userName: String) {
        roomMembers.computeIfAbsent(roomId) { CopyOnWriteArraySet() }.add(userId)

        // Notify others in room
        broadcastToRoom(roomId, "user_joined_room", RoomMemberAddedEvent(
            roomId = roomId,
            userId = userId,
            userName = userName,
            addedBy = userId
        ), excludeUser = userId)
    }

    suspend fun leaveRoom(roomId: String, userId: String, userName: String) {
        roomMembers[roomId]?.remove(userId)
        typingUsers[roomId]?.remove(userId)

        // Notify others in room
        broadcastToRoom(roomId, "user_left_room", RoomMemberRemovedEvent(
            roomId = roomId,
            userId = userId,
            userName = userName,
            removedBy = userId
        ), excludeUser = userId)
    }

    suspend fun broadcastNewMessage(message: NewMessageEvent) {
        broadcastToRoom(message.roomId, "new_message", message, excludeUser = message.senderId)
    }

    suspend fun broadcastMessageEdited(event: MessageEditedEvent) {
        broadcastToRoom(event.roomId, "message_edited", event)
    }

    suspend fun broadcastMessageDeleted(event: MessageDeletedEvent) {
        broadcastToRoom(event.roomId, "message_deleted", event)
    }

    suspend fun handleTyping(roomId: String, userId: String, userName: String, isTyping: Boolean) {
        if (isTyping) {
            typingUsers.computeIfAbsent(roomId) { CopyOnWriteArraySet() }.add(userId)
        } else {
            typingUsers[roomId]?.remove(userId)
        }

        val event = TypingEvent(
            roomId = roomId,
            userId = userId,
            userName = userName,
            isTyping = isTyping
        )

        broadcastToRoom(roomId, if (isTyping) "user_typing" else "user_stopped_typing", event, excludeUser = userId)
    }

    suspend fun broadcastMessageRead(event: MessageReadEvent) {
        broadcastToRoom(event.roomId, "message_read", event, excludeUser = event.userId)
    }

    suspend fun sendToUser(userId: String, type: String, data: Any) {
        val connectionId = userConnections[userId]
        if (connectionId != null) {
            val session = connections[connectionId]
            if (session != null) {
                try {
                    val message = WebSocketMessage(
                        type = type,
                        data = json.encodeToString(data)
                    )
                    session.send(Frame.Text(json.encodeToString(message)))
                } catch (e: Exception) {
                    println("Error sending message to user $userId: ${e.message}")
                    // Remove invalid connection
                    removeConnection(connectionId)
                }
            }
        }
    }

    suspend fun broadcastToRoom(roomId: String, type: String, data: Any, excludeUser: String? = null) {
        val members = roomMembers[roomId] ?: return

        members.forEach { userId ->
            if (userId != excludeUser) {
                sendToUser(userId, type, data)
            }
        }
    }

    suspend fun sendError(connectionId: String, error: String, code: String? = null) {
        val session = connections[connectionId]
        if (session != null) {
            try {
                val errorEvent = ErrorEvent(error = error, code = code)
                val message = WebSocketMessage(
                    type = "error",
                    data = json.encodeToString(errorEvent)
                )
                session.send(Frame.Text(json.encodeToString(message)))
            } catch (e: Exception) {
                println("Error sending error message: ${e.message}")
            }
        }
    }

    fun getOnlineUsers(): Set<String> = onlineUsers.toSet()

    fun getRoomMembers(roomId: String): Set<String> = roomMembers[roomId]?.toSet() ?: emptySet()

    fun getTypingUsers(roomId: String): Set<String> = typingUsers[roomId]?.toSet() ?: emptySet()

    fun isUserOnline(userId: String): Boolean = onlineUsers.contains(userId)
}