package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransportRouteDto(
    val id: String,
    val name: String,
    val description: String?,
    val isActive: Boolean
)

@Serializable
data class CreateTransportRouteRequest(
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true
)

@Serializable
data class UpdateTransportRouteRequest(
    val name: String,
    val description: String? = null,
    val isActive: Boolean
)