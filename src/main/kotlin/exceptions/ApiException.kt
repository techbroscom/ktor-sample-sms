package com.example.exceptions

import io.ktor.http.*

class ApiException(
    override val message: String,
    val statusCode: HttpStatusCode = HttpStatusCode.InternalServerError,
    val errorCode: String? = null
) : Exception(message)