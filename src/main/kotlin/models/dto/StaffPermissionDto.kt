package com.example.models.dto

data class StaffPermissionDto(
    val user: UserDto,
    val permissions: List<UserPermissionDto>
)
