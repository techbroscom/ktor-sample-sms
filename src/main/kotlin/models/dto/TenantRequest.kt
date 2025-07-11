package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateTenantRequest(
    val name: String,
)