package com.example.database.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

object VisitorPasses : Table("visitor_passes") {
    val id = integer("id").autoIncrement()
    val visitorId = uuid("visitor_id").references(Visitors.id, onDelete = ReferenceOption.CASCADE)
    val passNumber = varchar("pass_number", 50).uniqueIndex()
    val qrCodeUrl = varchar("qr_code_url", 500).nullable()
    val isActive = bool("is_active").default(true)
    val issuedAt = datetime("issued_at").default(LocalDateTime.now())
    val returnedAt = datetime("returned_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
