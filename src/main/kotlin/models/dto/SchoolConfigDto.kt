package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class SchoolConfigDto(
    val id: Int = 1,
    val schoolName: String,
    val address: String,
    val logoUrl: String? = null,
    val logoS3Key: String? = null,
    val email: String? = null,
    val phoneNumber1: String? = null,
    val phoneNumber2: String? = null,
    val phoneNumber3: String? = null,
    val phoneNumber4: String? = null,
    val phoneNumber5: String? = null,
    val website: String? = null
)

@Serializable
data class UpdateSchoolConfigRequest(
    val schoolName: String,
    val address: String,
    val logoUrl: String? = null,
    val logoS3Key: String? = null,
    val email: String? = null,
    val phoneNumber1: String? = null,
    val phoneNumber2: String? = null,
    val phoneNumber3: String? = null,
    val phoneNumber4: String? = null,
    val phoneNumber5: String? = null,
    val website: String? = null
)