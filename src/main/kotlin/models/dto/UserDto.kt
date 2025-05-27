package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: Int? = null,
    val name: String,
    val age: Int
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val age: Int
)

@Serializable
data class UpdateUserRequest(
    val name: String,
    val age: Int
)