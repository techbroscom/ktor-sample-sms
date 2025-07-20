package com.example.database.tables

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object ChatRoomMembers : Table("chat_room_members") {
    val id = uuid("id")
    val roomId = uuid("room_id").references(ChatRooms.id)
    val userId = uuid("user_id").references(Users.id)
    val role = enumerationByName("role", 20, ChatMemberRole::class).default(ChatMemberRole.MEMBER)
    val joinedAt = datetime("joined_at").default(LocalDateTime.now())
    val lastReadAt = datetime("last_read_at").nullable()
    val isActive = bool("is_active").default(true)

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex(roomId, userId)
    }
}