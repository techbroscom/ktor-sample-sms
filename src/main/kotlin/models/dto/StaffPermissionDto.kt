package com.example.models.dto

import kotlinx.serialization.Serializable

@Serializable
data class StaffPermissionDto(
    val user: UserDto,
    val permissions: List<UserPermissionDto>
)
