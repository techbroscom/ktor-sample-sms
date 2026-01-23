package com.example.database.tables

import org.jetbrains.exposed.sql.Table

object SchoolConfig : Table("school_config") {
    val id = integer("id").autoIncrement()
    val schoolName = varchar("school_name", 255)
    val address = text("address")
    val logoUrl = varchar("logo_url", 500).nullable()
    val logoS3Key = varchar("logo_s3_key", 500).nullable()
    val email = varchar("email", 255).nullable()
    val phoneNumber1 = varchar("phone_number_1", 20).nullable()
    val phoneNumber2 = varchar("phone_number_2", 20).nullable()
    val phoneNumber3 = varchar("phone_number_3", 20).nullable()
    val phoneNumber4 = varchar("phone_number_4", 20).nullable()
    val phoneNumber5 = varchar("phone_number_5", 20).nullable()
    val website = varchar("website", 255).nullable()

    override val primaryKey = PrimaryKey(id)
}
