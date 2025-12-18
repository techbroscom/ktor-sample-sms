package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class FileUploadResponse(
    val success: Boolean,
    val fileUrl: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val objectKey: String? = null,
    val message: String? = null
)

@Serializable
data class FileDeleteResponse(
    val success: Boolean,
    val message: String
)

data class UploadedFile(
    val fileName: String,
    val originalName: String,
    val fileSize: Long,
    val mimeType: String,
    val dropboxPath: String,
    val publicUrl: String
)
