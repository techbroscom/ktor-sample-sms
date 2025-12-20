package com.example.plugins

import com.example.exceptions.ApiException
import com.example.models.responses.ErrorDetail
import com.example.models.responses.ErrorResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, cause ->
            call.respond(
                cause.statusCode,
                ErrorResponse(
                    error = ErrorDetail(
                        code = cause.errorCode ?: cause.statusCode.value.toString(),
                        message = cause.message
                    )
                )
            )
        }

        exception<Throwable> { call, cause ->
            // Log the actual error for debugging
            call.application.environment.log.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error = ErrorDetail(
                        code = "INTERNAL_ERROR",
                        message = "An unexpected error occurred"
                    )
                )
            )
        }
    }
}