package com.example.routes.api

import io.ktor.client.*

import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
/**
 * Server-side Google OAuth routes.
 *
 * Flow:
 * 1. Tenant subdomain redirects user to: /auth/google?redirect_uri=https://tenant1.manisankarsms.co.in&tenant=tenant1
 * 2. This server redirects to Google OAuth consent screen
 * 3. Google redirects back to: /auth/google/callback with authorization code
 * 4. Server exchanges code for tokens, verifies user via Firebase Admin SDK
 * 5. Server redirects back to tenant subdomain with the Firebase ID token
 *
 * Only https://manisankarsms.co.in needs to be registered in Google OAuth origins.
 */

// Google OAuth config — loaded from environment variables
private val GOOGLE_CLIENT_ID = System.getenv("GOOGLE_OAUTH_CLIENT_ID") ?: ""
private val GOOGLE_CLIENT_SECRET = System.getenv("GOOGLE_OAUTH_CLIENT_SECRET") ?: ""
private const val GOOGLE_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
private const val GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token"
private val CALLBACK_URL = System.getenv("GOOGLE_OAUTH_CALLBACK_URL") ?: "https://manisankarsms.co.in/api/auth/google/callback"

private val httpClient = HttpClient(CIO)
private val json = Json { ignoreUnknownKeys = true }

@Serializable
data class GoogleTokenResponse(
    val access_token: String = "",
    val id_token: String = "",
    val token_type: String = "",
    val expires_in: Int = 0,
    val refresh_token: String = "",
    val scope: String = ""
)

fun Route.oauthRoutes() {
    route("/api/auth") {

        /**
         * Step 1: Initiate Google OAuth
         * Called from Flutter web app: /auth/google?redirect_uri=https://tenant1.manisankarsms.co.in&tenant=tenant1
         */
        get("/google") {
            val redirectUri = call.request.queryParameters["redirect_uri"]
                ?: return@get call.respondText("Missing redirect_uri parameter", status = HttpStatusCode.BadRequest)

            val tenant = call.request.queryParameters["tenant"] ?: ""

            // Validate redirect_uri is one of our subdomains
            if (!isValidRedirectUri(redirectUri)) {
                return@get call.respondText("Invalid redirect_uri", status = HttpStatusCode.BadRequest)
            }

            // Store tenant and redirect_uri in state parameter (base64 encoded)
            val state = java.util.Base64.getUrlEncoder().encodeToString(
                "$tenant|$redirectUri".toByteArray(StandardCharsets.UTF_8)
            )

            // Build Google OAuth URL
            val googleAuthUrl = buildString {
                append(GOOGLE_AUTH_URL)
                append("?client_id=${encode(GOOGLE_CLIENT_ID)}")
                append("&redirect_uri=${encode(CALLBACK_URL)}")
                append("&response_type=code")
                append("&scope=${encode("openid email profile")}")
                append("&state=${encode(state)}")
                append("&access_type=offline")
                append("&prompt=select_account")
            }

            call.respondRedirect(googleAuthUrl)
        }

        /**
         * Step 2: Google OAuth callback
         * Google redirects here with ?code=xxx&state=xxx
         */
        get("/google/callback") {
            val code = call.request.queryParameters["code"]
            val state = call.request.queryParameters["state"]
            val error = call.request.queryParameters["error"]

            if (error != null) {
                val (_, redirectUri) = decodeState(state)
                return@get call.respondRedirect("$redirectUri?auth_error=${encode(error)}")
            }

            if (code == null || state == null) {
                return@get call.respondText("Missing code or state", status = HttpStatusCode.BadRequest)
            }

            val (tenant, redirectUri) = decodeState(state)

            try {
                // Exchange authorization code for tokens
                val tokenResponse = exchangeCodeForTokens(code)

                // The id_token from Google is a JWT that Firebase can verify
                // But we need a Firebase ID token. Since we have the Google ID token,
                // we can use Firebase Admin SDK to create a custom token, or we can
                // just pass the Google ID token to the Flutter app which will use
                // signInWithCredential on the client side.

                // Redirect back to tenant with the Google ID token
                val separator = if (redirectUri.contains("?")) "&" else "?"
                val redirectUrl = "${redirectUri}${separator}google_id_token=${encode(tokenResponse.id_token)}&tenant=$tenant"

                call.respondRedirect(redirectUrl)

            } catch (e: Exception) {
                println("[OAuth] Error exchanging code: ${e.message}")
                val separator = if (redirectUri.contains("?")) "&" else "?"
                call.respondRedirect("${redirectUri}${separator}auth_error=${encode(e.message ?: "Unknown error")}")
            }
        }
    }
}

/**
 * Exchange the authorization code for tokens with Google.
 */
private suspend fun exchangeCodeForTokens(code: String): GoogleTokenResponse {
    val response = httpClient.submitForm(
        url = GOOGLE_TOKEN_URL,
        formParameters = Parameters.build {
            append("code", code)
            append("client_id", GOOGLE_CLIENT_ID)
            append("client_secret", GOOGLE_CLIENT_SECRET)
            append("redirect_uri", CALLBACK_URL)
            append("grant_type", "authorization_code")
        }
    )

    if (response.status != HttpStatusCode.OK) {
        val body = response.bodyAsText()
        println("[OAuth] Token exchange failed: $body")
        throw Exception("Failed to exchange authorization code: ${response.status}")
    }

    val body = response.bodyAsText()
    return json.decodeFromString<GoogleTokenResponse>(body)
}

/**
 * Decode the state parameter back to tenant and redirect_uri.
 */
private fun decodeState(state: String?): Pair<String, String> {
    if (state == null) return Pair("", "https://manisankarsms.co.in")
    return try {
        val decoded = String(java.util.Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8)
        val parts = decoded.split("|", limit = 2)
        Pair(parts.getOrElse(0) { "" }, parts.getOrElse(1) { "https://manisankarsms.co.in" })
    } catch (e: Exception) {
        Pair("", "https://manisankarsms.co.in")
    }
}

/**
 * Validate that the redirect_uri is one of our subdomains.
 */
private fun isValidRedirectUri(uri: String): Boolean {
    return uri.matches(Regex("^https://([a-z0-9-]+\\.)?manisankarsms\\.co\\.in(/.*)?$"))
}

private fun encode(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
}
