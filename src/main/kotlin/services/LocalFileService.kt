package com.example.services

import com.example.models.UploadedFile
import com.example.models.FileUploadResponse
import com.example.models.FileDeleteResponse
import org.apache.tika.Tika
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalFileService(
    private val baseUploadDir: String = "/var/www/uploads"
) {

    private val tika = Tika()

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
        private val ALLOWED_IMAGE_TYPES = setOf(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
        )
    }

    // ------------------------------
    // Save Profile Picture
    // ------------------------------
    suspend fun uploadProfilePicture(
        tenantId: String,
        userId: String,
        fileName: String,
        bytes: ByteArray
    ): FileUploadResponse {

        if (bytes.size > MAX_FILE_SIZE) {
            return FileUploadResponse(false, "File too large (max 10MB)")
        }

        val mime = tika.detect(bytes)
        if (mime !in ALLOWED_IMAGE_TYPES) {
            return FileUploadResponse(false, "Invalid image type: $mime")
        }

        val ext = fileName.substringAfterLast(".", "")
        val finalName = "profile_${userId}_${System.currentTimeMillis()}.$ext"

        val saved = saveFile(tenantId, "profile-pics", finalName, bytes)

        return FileUploadResponse(
            success = true,
            message = "Profile uploaded",
            fileUrl = saved.publicUrl,
            fileName = saved.fileName,
            fileSize = saved.fileSize
        )
    }

    // ------------------------------
    // Save Document
    // ------------------------------
    suspend fun uploadDocument(
        tenantId: String,
        userId: String,
        category: String,
        originalName: String,
        bytes: ByteArray
    ): FileUploadResponse {

        if (bytes.size > MAX_FILE_SIZE) {
            return FileUploadResponse(false, "File too large (max 10MB)")
        }

        val sanitized = category.replace(Regex("[^A-Za-z0-9]"), "_")
        val ext = originalName.substringAfterLast(".", "")
        val finalName = "${sanitized}_${userId}_${System.currentTimeMillis()}.$ext"

        val saved = saveFile(tenantId, "documents/$sanitized", finalName, bytes)

        return FileUploadResponse(
            success = true,
            message = "Document uploaded",
            fileUrl = saved.publicUrl,
            fileName = saved.fileName,
            fileSize = saved.fileSize
        )
    }

    // ------------------------------
    // Delete File
    // ------------------------------
    fun deleteFile(relativePath: String): FileDeleteResponse {
        val fullPath = "$baseUploadDir/$relativePath"

        val file = File(fullPath)
        return if (file.exists()) {
            file.delete()
            FileDeleteResponse(true, "File deleted")
        } else {
            FileDeleteResponse(false, "File not found")
        }
    }

    // ------------------------------
    // Core Save Logic
    // ------------------------------
    private suspend fun saveFile(
        tenantId: String,
        folder: String,
        finalName: String,
        bytes: ByteArray
    ): UploadedFile = withContext(Dispatchers.IO) {

        val fullDir = File("$baseUploadDir/$tenantId/$folder")
        if (!fullDir.exists()) fullDir.mkdirs()

        val finalPath = File(fullDir, finalName)
        finalPath.writeBytes(bytes)

        val publicUrl = "/uploads/$tenantId/$folder/$finalName"

        UploadedFile(
            fileName = finalName,
            originalName = finalName,
            fileSize = bytes.size.toLong(),
            mimeType = tika.detect(bytes),
            dropboxPath = publicUrl,
            publicUrl = publicUrl
        )
    }
}
