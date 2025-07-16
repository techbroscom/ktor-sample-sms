package com.example.models.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class TransportStopDto(
    val id: String,
    val routeId: String,
    val routeName: String, // Include route name for convenience
    val name: String,
    val orderIndex: Int,
    val monthlyFee: String, // String representation of BigDecimal
    val isActive: Boolean
)

@Serializable
data class CreateTransportStopRequest(
    val routeId: String,
    val name: String,
    val orderIndex: Int,
    val monthlyFee: String, // String representation of BigDecimal
    val isActive: Boolean = true
)

@Serializable
data class UpdateTransportStopRequest(
    val routeId: String,
    val name: String,
    val orderIndex: Int,
    val monthlyFee: String, // String representation of BigDecimal
    val isActive: Boolean
)