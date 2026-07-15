package com.example.services

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Zoho Webinar API integration service.
 *
 * Handles OAuth token management and webinar CRUD operations.
 * API base: https://webinar.zoho.com/api/v2/{zsoid}/...
 * Auth: OAuth 2.0 with refresh token flow.
 *
 * Key API details:
 * - Endpoints require .json suffix
 * - Request body wrapped in "session" object
 * - Duration is in milliseconds
 * - Start time format: "MMM dd, yyyy hh:mm a" (e.g., "Jul 20, 2026 10:00 AM")
 * - Response uses "meetingKey" as the webinar identifier
 */
class ZohoWebinarService {

    private val logger = LoggerFactory.getLogger(ZohoWebinarService::class.java)
    private val httpClient = HttpClient(CIO)
    private val json = Json { ignoreUnknownKeys = true }

    // Token cache
    @Volatile
    private var cachedAccessToken: String? = null
    @Volatile
    private var tokenExpiresAt: Long = 0

    // ============================================
    // OAuth Token Management
    // ============================================

    /**
     * Gets a valid access token, refreshing if expired.
     */
    suspend fun getAccessToken(credentials: ZohoWebinarCredentials): String {
        val now = System.currentTimeMillis()
        if (cachedAccessToken != null && now < tokenExpiresAt) {
            return cachedAccessToken!!
        }

        logger.info("Refreshing Zoho Webinar access token...")

        val response = httpClient.submitForm(
            url = "${credentials.accountsUrl}/oauth/v2/token",
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("client_id", credentials.clientId)
                append("client_secret", credentials.clientSecret)
                append("refresh_token", credentials.refreshToken)
            }
        )

        val body = response.bodyAsText()
        logger.debug("Token refresh response: {}", body)

        if (response.status != HttpStatusCode.OK) {
            logger.error("Failed to refresh Zoho token: {}", body)
            throw ZohoWebinarException("Failed to refresh access token: $body")
        }

        val tokenResponse = json.decodeFromString<ZohoTokenResponse>(body)
        cachedAccessToken = tokenResponse.access_token
        // Expire 2 minutes early to avoid edge cases
        tokenExpiresAt = now + (tokenResponse.expires_in * 1000) - 120_000

        logger.info("Zoho Webinar access token refreshed successfully")
        return cachedAccessToken!!
    }

    // ============================================
    // Webinar CRUD
    // ============================================

    /**
     * Creates a webinar on Zoho.
     *
     * @return ZohoWebinarResult with meetingKey, registrationLink, and startLink
     */
    suspend fun createWebinar(
        credentials: ZohoWebinarCredentials,
        request: ZohoCreateWebinarRequest
    ): ZohoWebinarResult {
        val token = getAccessToken(credentials)

        // Build the request body matching Zoho's expected format
        val requestBody = buildString {
            append("{\"session\":{")
            append("\"topic\":\"${escapeJson(request.topic)}\",")
            request.agenda?.let { append("\"agenda\":\"${escapeJson(it)}\",") }
            append("\"presenter\":${credentials.presenterZuid},")
            append("\"startTime\":\"${request.startTime}\",")
            append("\"duration\":${request.durationMs},")
            append("\"timezone\":\"${request.timezone}\"")
            append("}}")
        }

        logger.info("Creating Zoho webinar: ${request.topic}")
        logger.debug("Request body: $requestBody")

        val response = httpClient.post(
            "https://webinar.zoho.com/api/v2/${credentials.zsoid}/webinar.json"
        ) {
            header("Authorization", "Zoho-oauthtoken $token")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val body = response.bodyAsText()
        logger.debug("Create webinar response [${response.status}]: $body")

        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
            logger.error("Failed to create webinar: $body")
            throw ZohoWebinarException("Failed to create webinar: $body")
        }

        // Check for error in response body
        val jsonResponse = json.parseToJsonElement(body).jsonObject
        if (jsonResponse.containsKey("error")) {
            val errorMsg = jsonResponse["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                ?: body
            throw ZohoWebinarException("Zoho API error: $errorMsg")
        }

        val session = jsonResponse["session"]?.jsonObject
            ?: throw ZohoWebinarException("No 'session' in response: $body")

        val meetingKey = session["meetingKey"]?.jsonPrimitive?.content
            ?: throw ZohoWebinarException("No meetingKey in response: $body")

        val registrationLink = session["registrationLink"]?.jsonPrimitive?.content ?: ""
        val startLink = session["startLink"]?.jsonPrimitive?.content ?: ""

        // Extract instanceId (array with one element for non-recurring)
        val instanceId = session["instanceId"]?.let {
            (it as? kotlinx.serialization.json.JsonArray)?.firstOrNull()?.jsonPrimitive?.content
        } ?: ""

        logger.info("Webinar created: key=$meetingKey, instanceId=$instanceId, registration=$registrationLink")

        return ZohoWebinarResult(
            meetingKey = meetingKey,
            instanceId = instanceId,
            registrationLink = registrationLink,
            startLink = startLink,
            topic = request.topic
        )
    }

    /**
     * Gets webinar details by meetingKey.
     */
    suspend fun getWebinar(
        credentials: ZohoWebinarCredentials,
        meetingKey: String
    ): ZohoWebinarDetails {
        val token = getAccessToken(credentials)

        val response = httpClient.get(
            "https://webinar.zoho.com/api/v2/${credentials.zsoid}/webinar/$meetingKey.json"
        ) {
            header("Authorization", "Zoho-oauthtoken $token")
        }

        val body = response.bodyAsText()
        logger.debug("Get webinar response [${response.status}]: $body")

        if (response.status != HttpStatusCode.OK) {
            throw ZohoWebinarException("Failed to get webinar details: $body")
        }

        val jsonResponse = json.parseToJsonElement(body).jsonObject
        val session = jsonResponse["session"]?.jsonObject ?: jsonResponse

        return ZohoWebinarDetails(
            meetingKey = session["meetingKey"]?.jsonPrimitive?.content ?: meetingKey,
            topic = session["topic"]?.jsonPrimitive?.content ?: "",
            registrationLink = session["registrationLink"]?.jsonPrimitive?.content ?: "",
            startLink = session["startLink"]?.jsonPrimitive?.content ?: "",
            startTime = session["startTime"]?.jsonPrimitive?.content ?: "",
            endTime = session["endTime"]?.jsonPrimitive?.content ?: "",
            status = session["status"]?.jsonPrimitive?.content ?: "upcoming"
        )
    }

    /**
     * Registers an attendee for a webinar.
     * Returns the personalized join link.
     *
     * API: POST /api/v2/{zsoid}/register/{meetingKey}.json?instanceId={instanceId}&sendMail=false
     * Body key: "registrant" (singular!) — array of attendee objects
     */
    suspend fun registerAttendee(
        credentials: ZohoWebinarCredentials,
        meetingKey: String,
        instanceId: String,
        firstName: String,
        lastName: String?,
        email: String
    ): ZohoRegistrationResult {
        val token = getAccessToken(credentials)

        val lastNameField = lastName?.let { ",\"lastName\":\"${escapeJson(it)}\"" } ?: ""
        val requestBody = "{\"registrant\":[{\"email\":\"${escapeJson(email)}\",\"firstName\":\"${escapeJson(firstName)}\"$lastNameField}]}"

        logger.info("Registering attendee $email for webinar $meetingKey")

        val response = httpClient.post(
            "https://webinar.zoho.com/api/v2/${credentials.zsoid}/register/$meetingKey.json?instanceId=$instanceId&sendMail=false"
        ) {
            header("Authorization", "Zoho-oauthtoken $token")
            contentType(ContentType.Application.Json)
            setBody(requestBody)
        }

        val body = response.bodyAsText()
        logger.debug("Register attendee response [${response.status}]: $body")

        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Created) {
            logger.error("Failed to register attendee: $body")
            throw ZohoWebinarException("Failed to register attendee: $body")
        }

        // Parse response to get joinLink
        val jsonResponse = json.parseToJsonElement(body).jsonObject
        if (jsonResponse.containsKey("error")) {
            val errorMsg = jsonResponse["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: body
            throw ZohoWebinarException("Zoho registration error: $errorMsg")
        }

        val registrantArray = jsonResponse["registrant"]?.let {
            it as? kotlinx.serialization.json.JsonArray
        }
        val joinLink = registrantArray?.firstOrNull()?.jsonObject?.get("joinLink")?.jsonPrimitive?.content ?: ""

        logger.info("Attendee registered: $email -> joinLink obtained")

        return ZohoRegistrationResult(
            joinUrl = joinLink,
            email = email
        )
    }

    /**
     * Deletes a webinar.
     */
    suspend fun deleteWebinar(
        credentials: ZohoWebinarCredentials,
        meetingKey: String
    ) {
        val token = getAccessToken(credentials)

        val response = httpClient.delete(
            "https://webinar.zoho.com/api/v2/${credentials.zsoid}/webinar/$meetingKey.json"
        ) {
            header("Authorization", "Zoho-oauthtoken $token")
        }

        val body = response.bodyAsText()
        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.NoContent) {
            logger.error("Failed to delete webinar $meetingKey: $body")
            throw ZohoWebinarException("Failed to delete webinar: $body")
        }

        logger.info("Webinar deleted: $meetingKey")
    }

    // ============================================
    // Utility
    // ============================================

    /**
     * Formats date and time into Zoho's expected format.
     * Input: "2026-07-20", "10:00"
     * Output: "Jul 20, 2026 10:00 AM"
     */
    fun formatStartTime(date: String, time: String): String {
        val localDate = LocalDate.parse(date)
        val localTime = LocalTime.parse(time)
        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH)
        return localDate.atTime(localTime).format(formatter)
    }

    /**
     * Calculates duration in milliseconds between start and end time.
     */
    fun calculateDurationMs(startTime: String, endTime: String): Long {
        val start = LocalTime.parse(startTime)
        val end = LocalTime.parse(endTime)
        return java.time.Duration.between(start, end).toMillis()
    }

    private fun escapeJson(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Verifies credentials by fetching user info from Zoho.
     * Returns ZSOID, ZUID, org name, email — used during "Connect" flow.
     */
    suspend fun verifyAndFetchUserInfo(
        clientId: String,
        clientSecret: String,
        refreshToken: String,
        accountsUrl: String
    ): ZohoConnectResponse {
        // Build temporary credentials to get an access token
        val tempCredentials = ZohoWebinarCredentials(
            clientId = clientId,
            clientSecret = clientSecret,
            refreshToken = refreshToken,
            zsoid = "", // Not known yet
            presenterZuid = 0,
            accountsUrl = accountsUrl
        )

        val token = getAccessToken(tempCredentials)

        // Call /api/v2/user to get zsoid and zuid
        val response = httpClient.get("https://webinar.zoho.com/api/v2/user.json") {
            header("Authorization", "Zoho-oauthtoken $token")
        }

        val body = response.bodyAsText()
        logger.debug("Zoho user info response [${response.status}]: $body")

        if (response.status != HttpStatusCode.OK) {
            throw ZohoWebinarException("Failed to fetch Zoho user info: $body")
        }

        val jsonResponse = json.parseToJsonElement(body).jsonObject
        val userDetails = jsonResponse["userDetails"]?.jsonObject
            ?: throw ZohoWebinarException("No userDetails in response: $body")

        val zsoid = userDetails["zsoid"]?.jsonPrimitive?.content
            ?: throw ZohoWebinarException("No zsoid in response")
        val zuid = userDetails["zuid"]?.jsonPrimitive?.content?.toLongOrNull()
            ?: throw ZohoWebinarException("No zuid in response")
        val orgName = userDetails["orgName"]?.jsonPrimitive?.content ?: ""
        val email = userDetails["primaryEmail"]?.jsonPrimitive?.content ?: ""
        val displayName = userDetails["displayName"]?.jsonPrimitive?.content ?: ""

        return ZohoConnectResponse(
            zsoid = zsoid,
            presenterZuid = zuid,
            orgName = orgName,
            email = email,
            displayName = displayName
        )
    }
}

// ============================================
// Data Classes
// ============================================

@Serializable
data class ZohoWebinarCredentials(
    val clientId: String,
    val clientSecret: String,
    val refreshToken: String,
    val zsoid: String,
    val presenterZuid: Long,  // Zoho User ID of the default presenter
    val accountsUrl: String = "https://accounts.zoho.com"
)

@Serializable
data class ZohoTokenResponse(
    val access_token: String,
    val token_type: String = "Bearer",
    val expires_in: Long = 3600,
    val scope: String? = null,
    val api_domain: String? = null
)

data class ZohoCreateWebinarRequest(
    val topic: String,
    val agenda: String? = null,
    val startTime: String,   // "Jul 20, 2026 10:00 AM"
    val durationMs: Long,    // Duration in milliseconds
    val timezone: String = "Asia/Kolkata"
)

data class ZohoWebinarResult(
    val meetingKey: String,
    val instanceId: String,
    val registrationLink: String,
    val startLink: String,
    val topic: String
)

data class ZohoWebinarDetails(
    val meetingKey: String,
    val topic: String,
    val registrationLink: String,
    val startLink: String,
    val startTime: String,
    val endTime: String,
    val status: String
)

data class ZohoRegistrationResult(
    val joinUrl: String,
    val email: String
)

class ZohoWebinarException(message: String) : RuntimeException(message)

@Serializable
data class ZohoConnectRequest(
    val clientId: String,
    val clientSecret: String,
    val refreshToken: String,
    val accountsUrl: String = "https://accounts.zoho.com"
)

@Serializable
data class ZohoConnectResponse(
    val zsoid: String,
    val presenterZuid: Long,
    val orgName: String,
    val email: String,
    val displayName: String
)
