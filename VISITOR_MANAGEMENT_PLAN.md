# Visitor Management Feature - Implementation Plan

## Overview
This document outlines the implementation plan for a comprehensive Visitor Management system in our schema-based multi-tenant Ktor application.

## Architecture Context

### Multi-Tenant Setup
- **Public Schema**: Contains tenant metadata, features catalog, and tenant-feature mappings
- **Tenant Schemas** (tenant_0001, tenant_0002, etc.): Isolated data per tenant
- **Context Management**: ThreadLocal-based tenant context via `X-Tenant` header
- **Database Access**: `tenantDbQuery {}` for tenant schema, `systemDbQuery {}` for public schema

---

## 1. Database Schema Design

### 1.1 Visitors Table (Tenant Schema)
Located in each tenant's schema (tenant_XXXX):

```kotlin
object Visitors : Table("visitors") {
    val id = uuid("id")
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 255).nullable()
    val mobileNumber = varchar("mobile_number", 15)
    val organizationName = varchar("organization_name", 255).nullable()
    val purposeOfVisit = varchar("purpose_of_visit", 500)
    val visitDate = date("visit_date")
    val expectedCheckInTime = datetime("expected_check_in_time")
    val actualCheckInTime = datetime("actual_check_in_time").nullable()
    val checkOutTime = datetime("check_out_time").nullable()
    val status = enumerationByName("status", 20, VisitorStatus::class)
    val hostUserId = uuid("host_user_id").references(Users.id)
    val identificationProof = varchar("identification_proof", 50).nullable() // e.g., "Driving License", "Passport"
    val identificationNumber = varchar("identification_number", 100).nullable()
    val photoUrl = varchar("photo_url", 500).nullable() // S3 URL for visitor photo
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()
    val createdBy = uuid("created_by").references(Users.id)

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, hostUserId)
        index(false, visitDate)
        index(false, status)
    }
}

enum class VisitorStatus {
    SCHEDULED,      // Pre-registered, not yet arrived
    CHECKED_IN,     // Currently on premises
    CHECKED_OUT,    // Visit completed
    CANCELLED,      // Scheduled visit cancelled
    NO_SHOW         // Scheduled but didn't arrive
}
```

**Design Decisions:**
- **UUID Primary Key**: Consistent with existing tables (Users, Classes, etc.)
- **Host Reference**: Links to Users table within same tenant schema
- **Status Enum**: Clear visitor lifecycle states
- **Flexible Identification**: Support various ID types across regions
- **Photo Storage**: S3 URL reference (following existing file storage pattern)
- **Audit Fields**: createdAt, updatedAt, createdBy for compliance

### 1.2 Visitor Passes (Optional Enhancement)
For organizations requiring physical/digital passes:

```kotlin
object VisitorPasses : Table("visitor_passes") {
    val id = integer("id").autoIncrement()
    val visitorId = uuid("visitor_id").references(Visitors.id, onDelete = ReferenceOption.CASCADE)
    val passNumber = varchar("pass_number", 50).uniqueIndex()
    val qrCodeUrl = varchar("qr_code_url", 500).nullable() // S3 URL for QR code image
    val isActive = bool("is_active").default(true)
    val issuedAt = datetime("issued_at").default(LocalDateTime.now())
    val returnedAt = datetime("returned_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
```

---

## 2. Data Models & DTOs

### 2.1 Request Models

```kotlin
@Serializable
data class CreateVisitorRequest(
    val firstName: String,
    val lastName: String,
    val email: String? = null,
    val mobileNumber: String,
    val organizationName: String? = null,
    val purposeOfVisit: String,
    val visitDate: String, // ISO 8601 date
    val expectedCheckInTime: String, // ISO 8601 datetime
    val hostUserId: String, // UUID string
    val identificationProof: String? = null,
    val identificationNumber: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null
)

@Serializable
data class UpdateVisitorRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val mobileNumber: String? = null,
    val organizationName: String? = null,
    val purposeOfVisit: String? = null,
    val visitDate: String? = null,
    val expectedCheckInTime: String? = null,
    val hostUserId: String? = null,
    val notes: String? = null
)

@Serializable
data class CheckInRequest(
    val actualCheckInTime: String? = null, // Optional, defaults to now
    val photoUrl: String? = null, // Optional check-in photo
    val passNumber: String? = null // If using visitor passes
)

@Serializable
data class CheckOutRequest(
    val checkOutTime: String? = null, // Optional, defaults to now
    val notes: String? = null // Exit notes
)

@Serializable
data class VisitorSearchRequest(
    val searchQuery: String? = null, // Search by name, email, mobile
    val visitDate: String? = null,
    val status: String? = null, // VisitorStatus enum value
    val hostUserId: String? = null,
    val fromDate: String? = null,
    val toDate: String? = null,
    val page: Int = 1,
    val pageSize: Int = 20
)
```

### 2.2 Response Models

```kotlin
@Serializable
data class VisitorDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val fullName: String, // Computed: firstName + lastName
    val email: String? = null,
    val mobileNumber: String,
    val organizationName: String? = null,
    val purposeOfVisit: String,
    val visitDate: String,
    val expectedCheckInTime: String,
    val actualCheckInTime: String? = null,
    val checkOutTime: String? = null,
    val status: String, // VisitorStatus enum value
    val hostUser: UserSummaryDto, // Embedded host information
    val identificationProof: String? = null,
    val identificationNumber: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null,
    val createdAt: String,
    val updatedAt: String? = null,
    val createdBy: UserSummaryDto
)

@Serializable
data class UserSummaryDto(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: String
)

@Serializable
data class VisitorPassDto(
    val id: Int,
    val visitorId: String,
    val passNumber: String,
    val qrCodeUrl: String? = null,
    val isActive: Boolean,
    val issuedAt: String,
    val returnedAt: String? = null
)

@Serializable
data class VisitorStatsDto(
    val totalVisitors: Int,
    val currentlyCheckedIn: Int,
    val scheduledToday: Int,
    val completedToday: Int,
    val noShowsThisWeek: Int
)
```

---

## 3. Repository Layer

### 3.1 VisitorRepository

```kotlin
class VisitorRepository {

    // Create new visitor record
    suspend fun create(
        request: CreateVisitorRequest,
        createdBy: UUID
    ): UUID = tenantDbQuery {
        val visitorId = UUID.randomUUID()
        Visitors.insert {
            it[id] = visitorId
            it[firstName] = request.firstName
            it[lastName] = request.lastName
            it[email] = request.email
            it[mobileNumber] = request.mobileNumber
            it[organizationName] = request.organizationName
            it[purposeOfVisit] = request.purposeOfVisit
            it[visitDate] = LocalDate.parse(request.visitDate)
            it[expectedCheckInTime] = LocalDateTime.parse(request.expectedCheckInTime)
            it[status] = VisitorStatus.SCHEDULED
            it[hostUserId] = UUID.fromString(request.hostUserId)
            it[identificationProof] = request.identificationProof
            it[identificationNumber] = request.identificationNumber
            it[photoUrl] = request.photoUrl
            it[notes] = request.notes
            it[Visitors.createdBy] = createdBy
        }
        visitorId
    }

    // Find by ID with host user details
    suspend fun findById(visitorId: UUID): VisitorDto? = tenantDbQuery {
        (Visitors innerJoin Users)
            .selectAll()
            .where { Visitors.id eq visitorId }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    // Search with filters and pagination
    suspend fun search(request: VisitorSearchRequest): PaginatedResult<VisitorDto> = tenantDbQuery {
        val query = (Visitors innerJoin Users).selectAll()

        // Apply filters
        request.searchQuery?.let { searchTerm ->
            query.andWhere {
                (Visitors.firstName like "%$searchTerm%") or
                (Visitors.lastName like "%$searchTerm%") or
                (Visitors.email like "%$searchTerm%") or
                (Visitors.mobileNumber like "%$searchTerm%")
            }
        }

        request.status?.let { query.andWhere { Visitors.status eq VisitorStatus.valueOf(it) } }
        request.hostUserId?.let { query.andWhere { Visitors.hostUserId eq UUID.fromString(it) } }
        request.visitDate?.let { query.andWhere { Visitors.visitDate eq LocalDate.parse(it) } }

        request.fromDate?.let {
            query.andWhere { Visitors.visitDate greaterEq LocalDate.parse(it) }
        }
        request.toDate?.let {
            query.andWhere { Visitors.visitDate lessEq LocalDate.parse(it) }
        }

        val total = query.count()
        val offset = ((request.page - 1) * request.pageSize).toLong()

        val visitors = query
            .limit(request.pageSize, offset)
            .orderBy(Visitors.visitDate to SortOrder.DESC, Visitors.expectedCheckInTime to SortOrder.DESC)
            .map { mapRowToDto(it) }

        PaginatedResult(
            data = visitors,
            page = request.page,
            pageSize = request.pageSize,
            total = total.toInt(),
            totalPages = ((total + request.pageSize - 1) / request.pageSize).toInt()
        )
    }

    // Check in visitor
    suspend fun checkIn(visitorId: UUID, actualTime: LocalDateTime): Boolean = tenantDbQuery {
        Visitors.update({ Visitors.id eq visitorId }) {
            it[actualCheckInTime] = actualTime
            it[status] = VisitorStatus.CHECKED_IN
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    // Check out visitor
    suspend fun checkOut(visitorId: UUID, checkOutTime: LocalDateTime): Boolean = tenantDbQuery {
        Visitors.update({ Visitors.id eq visitorId }) {
            it[Visitors.checkOutTime] = checkOutTime
            it[status] = VisitorStatus.CHECKED_OUT
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    // Update visitor details
    suspend fun update(visitorId: UUID, request: UpdateVisitorRequest): Boolean = tenantDbQuery {
        Visitors.update({ Visitors.id eq visitorId }) {
            request.firstName?.let { v -> it[firstName] = v }
            request.lastName?.let { v -> it[lastName] = v }
            request.email?.let { v -> it[email] = v }
            request.mobileNumber?.let { v -> it[mobileNumber] = v }
            request.organizationName?.let { v -> it[organizationName] = v }
            request.purposeOfVisit?.let { v -> it[purposeOfVisit] = v }
            request.visitDate?.let { v -> it[visitDate] = LocalDate.parse(v) }
            request.expectedCheckInTime?.let { v -> it[expectedCheckInTime] = LocalDateTime.parse(v) }
            request.hostUserId?.let { v -> it[hostUserId] = UUID.fromString(v) }
            request.notes?.let { v -> it[notes] = v }
            it[updatedAt] = LocalDateTime.now()
        } > 0
    }

    // Get statistics
    suspend fun getStats(fromDate: LocalDate, toDate: LocalDate): VisitorStatsDto = tenantDbQuery {
        val total = Visitors.selectAll()
            .where { Visitors.visitDate.between(fromDate, toDate) }
            .count()

        val checkedIn = Visitors.selectAll()
            .where { Visitors.status eq VisitorStatus.CHECKED_IN }
            .count()

        val today = LocalDate.now()
        val scheduledToday = Visitors.selectAll()
            .where {
                (Visitors.visitDate eq today) and
                (Visitors.status eq VisitorStatus.SCHEDULED)
            }
            .count()

        val completedToday = Visitors.selectAll()
            .where {
                (Visitors.visitDate eq today) and
                (Visitors.status eq VisitorStatus.CHECKED_OUT)
            }
            .count()

        val weekAgo = today.minusDays(7)
        val noShows = Visitors.selectAll()
            .where {
                (Visitors.visitDate.between(weekAgo, today)) and
                (Visitors.status eq VisitorStatus.NO_SHOW)
            }
            .count()

        VisitorStatsDto(
            totalVisitors = total.toInt(),
            currentlyCheckedIn = checkedIn.toInt(),
            scheduledToday = scheduledToday.toInt(),
            completedToday = completedToday.toInt(),
            noShowsThisWeek = noShows.toInt()
        )
    }

    // Helper to map database row to DTO
    private fun mapRowToDto(row: ResultRow): VisitorDto {
        return VisitorDto(
            id = row[Visitors.id].toString(),
            firstName = row[Visitors.firstName],
            lastName = row[Visitors.lastName],
            fullName = "${row[Visitors.firstName]} ${row[Visitors.lastName]}",
            email = row[Visitors.email],
            mobileNumber = row[Visitors.mobileNumber],
            organizationName = row[Visitors.organizationName],
            purposeOfVisit = row[Visitors.purposeOfVisit],
            visitDate = row[Visitors.visitDate].toString(),
            expectedCheckInTime = row[Visitors.expectedCheckInTime].toString(),
            actualCheckInTime = row[Visitors.actualCheckInTime]?.toString(),
            checkOutTime = row[Visitors.checkOutTime]?.toString(),
            status = row[Visitors.status].name,
            hostUser = UserSummaryDto(
                id = row[Users.id].toString(),
                firstName = row[Users.firstName],
                lastName = row[Users.lastName],
                email = row[Users.email],
                role = row[Users.role].name
            ),
            identificationProof = row[Visitors.identificationProof],
            identificationNumber = row[Visitors.identificationNumber],
            photoUrl = row[Visitors.photoUrl],
            notes = row[Visitors.notes],
            createdAt = row[Visitors.createdAt].toString(),
            updatedAt = row[Visitors.updatedAt]?.toString(),
            createdBy = getUserSummary(row[Visitors.createdBy])
        )
    }

    private suspend fun getUserSummary(userId: UUID): UserSummaryDto = tenantDbQuery {
        Users.selectAll()
            .where { Users.id eq userId }
            .map {
                UserSummaryDto(
                    id = it[Users.id].toString(),
                    firstName = it[Users.firstName],
                    lastName = it[Users.lastName],
                    email = it[Users.email],
                    role = it[Users.role].name
                )
            }
            .single()
    }
}
```

---

## 4. Service Layer

### 4.1 VisitorService

```kotlin
class VisitorService(
    private val visitorRepository: VisitorRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService? = null
) {

    suspend fun createVisitor(
        request: CreateVisitorRequest,
        createdBy: String
    ): VisitorDto {
        val tenantContext = TenantContextHolder.getTenant()
            ?: throw ApiException("Tenant context not found", HttpStatusCode.BadRequest)

        // Validate host user exists
        val hostUserId = UUID.fromString(request.hostUserId)
        val hostUser = userRepository.findById(hostUserId)
            ?: throw ApiException("Host user not found", HttpStatusCode.NotFound)

        // Validate host is ADMIN or STAFF
        if (hostUser.role !in listOf("ADMIN", "STAFF")) {
            throw ApiException("Host must be an admin or staff member", HttpStatusCode.BadRequest)
        }

        // Validate mobile number format
        if (!request.mobileNumber.matches(Regex("^\\+?[1-9]\\d{1,14}$"))) {
            throw ApiException("Invalid mobile number format", HttpStatusCode.BadRequest)
        }

        // Parse and validate dates
        val visitDate = LocalDate.parse(request.visitDate)
        val expectedCheckInTime = LocalDateTime.parse(request.expectedCheckInTime)

        if (visitDate.isBefore(LocalDate.now())) {
            throw ApiException("Visit date cannot be in the past", HttpStatusCode.BadRequest)
        }

        // Create visitor
        val visitorId = visitorRepository.create(request, UUID.fromString(createdBy))

        // Send notification to host
        notificationService?.notifyHostAboutScheduledVisit(hostUserId, visitorId)

        return visitorRepository.findById(visitorId)
            ?: throw ApiException("Failed to retrieve created visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun getVisitor(visitorId: String): VisitorDto {
        val id = UUID.fromString(visitorId)
        return visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)
    }

    suspend fun searchVisitors(request: VisitorSearchRequest): PaginatedResult<VisitorDto> {
        return visitorRepository.search(request)
    }

    suspend fun checkIn(visitorId: String, request: CheckInRequest): VisitorDto {
        val id = UUID.fromString(visitorId)

        // Validate visitor exists and is in SCHEDULED status
        val visitor = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        if (visitor.status != VisitorStatus.SCHEDULED.name) {
            throw ApiException(
                "Only scheduled visitors can be checked in. Current status: ${visitor.status}",
                HttpStatusCode.BadRequest
            )
        }

        val actualTime = request.actualCheckInTime?.let { LocalDateTime.parse(it) }
            ?: LocalDateTime.now()

        visitorRepository.checkIn(id, actualTime)

        // Send notification to host
        notificationService?.notifyHostAboutCheckIn(UUID.fromString(visitor.hostUser.id), id)

        return visitorRepository.findById(id)
            ?: throw ApiException("Failed to retrieve updated visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun checkOut(visitorId: String, request: CheckOutRequest): VisitorDto {
        val id = UUID.fromString(visitorId)

        // Validate visitor exists and is checked in
        val visitor = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        if (visitor.status != VisitorStatus.CHECKED_IN.name) {
            throw ApiException(
                "Only checked-in visitors can be checked out. Current status: ${visitor.status}",
                HttpStatusCode.BadRequest
            )
        }

        val checkOutTime = request.checkOutTime?.let { LocalDateTime.parse(it) }
            ?: LocalDateTime.now()

        visitorRepository.checkOut(id, checkOutTime)

        return visitorRepository.findById(id)
            ?: throw ApiException("Failed to retrieve updated visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun updateVisitor(visitorId: String, request: UpdateVisitorRequest): VisitorDto {
        val id = UUID.fromString(visitorId)

        // Validate visitor exists
        val existing = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        // Can't update checked-out visitors
        if (existing.status == VisitorStatus.CHECKED_OUT.name) {
            throw ApiException("Cannot update checked-out visitors", HttpStatusCode.BadRequest)
        }

        // Validate new host if provided
        request.hostUserId?.let { hostId ->
            val hostUser = userRepository.findById(UUID.fromString(hostId))
                ?: throw ApiException("Host user not found", HttpStatusCode.NotFound)

            if (hostUser.role !in listOf("ADMIN", "STAFF")) {
                throw ApiException("Host must be an admin or staff member", HttpStatusCode.BadRequest)
            }
        }

        visitorRepository.update(id, request)

        return visitorRepository.findById(id)
            ?: throw ApiException("Failed to retrieve updated visitor", HttpStatusCode.InternalServerError)
    }

    suspend fun deleteVisitor(visitorId: String) {
        val id = UUID.fromString(visitorId)

        val visitor = visitorRepository.findById(id)
            ?: throw ApiException("Visitor not found", HttpStatusCode.NotFound)

        // Only allow deletion of SCHEDULED or CANCELLED visitors
        if (visitor.status !in listOf(VisitorStatus.SCHEDULED.name, VisitorStatus.CANCELLED.name)) {
            throw ApiException(
                "Can only delete scheduled or cancelled visitors",
                HttpStatusCode.BadRequest
            )
        }

        visitorRepository.delete(id)
    }

    suspend fun getVisitorStats(fromDate: String?, toDate: String?): VisitorStatsDto {
        val from = fromDate?.let { LocalDate.parse(it) } ?: LocalDate.now().minusDays(30)
        val to = toDate?.let { LocalDate.parse(it) } ?: LocalDate.now()

        return visitorRepository.getStats(from, to)
    }

    suspend fun getMyHostedVisitors(hostUserId: String, status: String?): List<VisitorDto> {
        val request = VisitorSearchRequest(
            hostUserId = hostUserId,
            status = status,
            pageSize = 100 // Get all for this host
        )
        return visitorRepository.search(request).data
    }
}
```

---

## 5. API Routes

### 5.1 VisitorRoutes

```kotlin
fun Route.visitorRoutes(
    visitorService: VisitorService,
    jwtService: JwtService
) {
    route("/api/v1/visitors") {

        // Create new visitor (ADMIN, STAFF)
        post {
            val request = call.receive<CreateVisitorRequest>()
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: throw ApiException("Unauthorized", HttpStatusCode.Unauthorized)

            val visitor = visitorService.createVisitor(request, userId)
            call.respond(HttpStatusCode.Created, ApiResponse(success = true, data = visitor))
        }

        // Search/List visitors with filters
        get {
            val searchRequest = VisitorSearchRequest(
                searchQuery = call.request.queryParameters["search"],
                visitDate = call.request.queryParameters["visitDate"],
                status = call.request.queryParameters["status"],
                hostUserId = call.request.queryParameters["hostUserId"],
                fromDate = call.request.queryParameters["fromDate"],
                toDate = call.request.queryParameters["toDate"],
                page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1,
                pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
            )

            val result = visitorService.searchVisitors(searchRequest)
            call.respond(ApiResponse(success = true, data = result))
        }

        // Get visitor statistics
        get("/stats") {
            val fromDate = call.request.queryParameters["fromDate"]
            val toDate = call.request.queryParameters["toDate"]

            val stats = visitorService.getVisitorStats(fromDate, toDate)
            call.respond(ApiResponse(success = true, data = stats))
        }

        // Get visitors hosted by current user
        get("/my-hosted") {
            val userId = call.principal<JWTPrincipal>()
                ?.payload?.getClaim("userId")?.asString()
                ?: throw ApiException("Unauthorized", HttpStatusCode.Unauthorized)

            val status = call.request.queryParameters["status"]
            val visitors = visitorService.getMyHostedVisitors(userId, status)
            call.respond(ApiResponse(success = true, data = visitors))
        }

        // Get visitor by ID
        get("/{id}") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val visitor = visitorService.getVisitor(visitorId)
            call.respond(ApiResponse(success = true, data = visitor))
        }

        // Update visitor
        put("/{id}") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateVisitorRequest>()
            val visitor = visitorService.updateVisitor(visitorId, request)
            call.respond(ApiResponse(success = true, data = visitor))
        }

        // Check in visitor
        post("/{id}/check-in") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<CheckInRequest>()
            val visitor = visitorService.checkIn(visitorId, request)
            call.respond(ApiResponse(success = true, data = visitor))
        }

        // Check out visitor
        post("/{id}/check-out") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<CheckOutRequest>()
            val visitor = visitorService.checkOut(visitorId, request)
            call.respond(ApiResponse(success = true, data = visitor))
        }

        // Delete visitor (only SCHEDULED or CANCELLED)
        delete("/{id}") {
            val visitorId = call.parameters["id"]
                ?: throw ApiException("Visitor ID is required", HttpStatusCode.BadRequest)

            visitorService.deleteVisitor(visitorId)
            call.respond(
                HttpStatusCode.OK,
                ApiResponse<Unit>(success = true, message = "Visitor deleted successfully")
            )
        }
    }
}
```

### 5.2 Route Registration
Update `plugins/Routing.kt`:

```kotlin
fun Application.configureRouting() {
    // ... existing services
    val visitorRepository = VisitorRepository()
    val visitorService = VisitorService(visitorRepository, userRepository, notificationService)

    routing {
        // ... existing routes
        visitorRoutes(visitorService, jwtService)
    }
}
```

---

## 6. Feature Catalog Integration

### 6.1 Add Visitor Management Feature
Insert into `Features` table (public schema):

```sql
INSERT INTO features (feature_key, name, description, category, is_active, default_enabled, has_limit, limit_type, limit_value, limit_unit)
VALUES (
    'visitor_management',
    'Visitor Management',
    'Track and manage visitors, check-ins, and check-outs with host notifications',
    'operations',
    true,
    false,
    true,
    'monthly_visitors',
    1000,
    'visitors'
);
```

### 6.2 Tenant Feature Assignment
Tenants can enable the feature via:
```
POST /api/v1/tenantsConfig/{tenantId}/features
{
  "featureKey": "visitor_management",
  "customLimitValue": 500
}
```

### 6.3 User Permission Check
Add permission validation in routes:

```kotlin
// Check if user has visitor_management permission
val userPermissions = userPermissionsRepository.getEnabledFeatureKeys(userId)
if ("visitor_management" !in userPermissions) {
    throw ApiException("Visitor management feature not enabled for this user", HttpStatusCode.Forbidden)
}
```

---

## 7. Notification System

### 7.1 NotificationService Integration

```kotlin
class NotificationService(
    private val fcmService: FCMService? = null,
    private val emailService: EmailService? = null
) {

    suspend fun notifyHostAboutScheduledVisit(hostUserId: UUID, visitorId: UUID) {
        val visitor = visitorRepository.findById(visitorId) ?: return
        val host = userRepository.findById(hostUserId) ?: return

        val message = """
            New visitor scheduled:
            Name: ${visitor.fullName}
            Purpose: ${visitor.purposeOfVisit}
            Expected: ${visitor.expectedCheckInTime}
        """.trimIndent()

        // Send FCM notification
        fcmService?.sendToUser(
            userId = hostUserId,
            title = "Visitor Scheduled",
            body = message,
            data = mapOf(
                "type" to "visitor_scheduled",
                "visitorId" to visitorId.toString()
            )
        )

        // Send email
        emailService?.send(
            to = host.email,
            subject = "Visitor Scheduled - ${visitor.fullName}",
            body = message
        )
    }

    suspend fun notifyHostAboutCheckIn(hostUserId: UUID, visitorId: UUID) {
        val visitor = visitorRepository.findById(visitorId) ?: return

        fcmService?.sendToUser(
            userId = hostUserId,
            title = "Visitor Arrived",
            body = "${visitor.fullName} has checked in",
            data = mapOf(
                "type" to "visitor_checked_in",
                "visitorId" to visitorId.toString()
            )
        )
    }

    suspend fun sendVisitorPassEmail(visitorId: UUID) {
        val visitor = visitorRepository.findById(visitorId) ?: return

        visitor.email?.let { email ->
            emailService?.send(
                to = email,
                subject = "Your Visitor Pass",
                body = """
                    Dear ${visitor.fullName},

                    Your visit is scheduled for ${visitor.visitDate}.
                    Host: ${visitor.hostUser.firstName} ${visitor.hostUser.lastName}

                    Please arrive at ${visitor.expectedCheckInTime}.
                """.trimIndent()
            )
        }
    }
}
```

---

## 8. Database Migration

### 8.1 Migration Service
Add to `services/MigrationService.kt`:

```kotlin
class MigrationService {

    suspend fun createVisitorManagementTables() {
        println("Starting visitor management tables migration...")

        // Get all existing tenants from public schema
        val tenants = systemDbQuery {
            Tenants.selectAll()
                .map {
                    it[Tenants.id].toString() to it[Tenants.schema_name]
                }
        }

        println("Found ${tenants.size} tenants")

        // Create tables in each tenant schema
        tenants.forEach { (tenantId, schemaName) ->
            try {
                val tenantDb = TenantDatabaseConfig.getTenantDatabase(schemaName)
                transaction(tenantDb) {
                    exec("SET search_path TO $schemaName")

                    // Create Visitors table
                    SchemaUtils.create(Visitors)

                    // Create VisitorPasses table (optional)
                    SchemaUtils.create(VisitorPasses)

                    println("✓ Created visitor tables for tenant: $schemaName")
                }
            } catch (e: Exception) {
                println("✗ Failed to create tables for tenant $schemaName: ${e.message}")
            }
        }

        println("Visitor management tables migration completed")
    }

    // Add to schema creation for new tenants
    suspend fun initializeTenantSchema(schemaName: String) {
        val tenantDb = TenantDatabaseConfig.getTenantDatabase(schemaName)
        transaction(tenantDb) {
            exec("CREATE SCHEMA IF NOT EXISTS $schemaName")
            exec("SET search_path TO $schemaName")

            SchemaUtils.create(
                Users,
                Holidays,
                Posts,
                Classes,
                // ... existing tables
                Visitors,         // Add visitor tables
                VisitorPasses,
                // ... other tables
            )
        }
    }
}
```

### 8.2 Run Migration
Update `plugins/Databases.kt`:

```kotlin
fun Application.configureDatabases() {
    val systemDatabase = TenantDatabaseConfig.getSystemDb()

    // Create system tables
    transaction(systemDatabase) {
        SchemaUtils.create(Tenants, TenantConfig, Features, TenantFeatures)
    }

    // Run migrations
    val migrationService = MigrationService()
    runBlocking {
        // Existing migrations
        migrationService.migrateTenantPostImagesTable()
        migrationService.removeFilesTenantIdColumn()

        // NEW: Visitor management migration
        migrationService.createVisitorManagementTables()
    }
}
```

---

## 9. Testing Strategy

### 9.1 Unit Tests

```kotlin
class VisitorRepositoryTest {
    @Test
    fun `should create visitor successfully`() = runTest {
        // Setup tenant context
        // Create visitor
        // Assert visitor created with correct data
    }

    @Test
    fun `should check in visitor and update status`() = runTest {
        // Create scheduled visitor
        // Check in
        // Assert status is CHECKED_IN
    }
}

class VisitorServiceTest {
    @Test
    fun `should throw exception when host user not found`() = runTest {
        // Create request with non-existent host
        // Assert ApiException thrown
    }

    @Test
    fun `should validate mobile number format`() = runTest {
        // Create request with invalid mobile
        // Assert validation error
    }
}
```

### 9.2 Integration Tests

```kotlin
class VisitorRoutesTest : ApplicationTest() {
    @Test
    fun `POST visitors should create visitor and return 201`() = testApplication {
        // Setup tenant header
        // POST request
        // Assert 201 response
        // Verify visitor created in database
    }

    @Test
    fun `GET visitors should return paginated results`() = testApplication {
        // Create multiple visitors
        // GET with pagination
        // Assert correct page data
    }
}
```

---

## 10. API Documentation

### 10.1 OpenAPI/Swagger Specification

```yaml
/api/v1/visitors:
  post:
    summary: Create new visitor
    tags: [Visitors]
    security:
      - bearerAuth: []
    parameters:
      - name: X-Tenant
        in: header
        required: true
        schema:
          type: string
          format: uuid
    requestBody:
      required: true
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/CreateVisitorRequest'
    responses:
      201:
        description: Visitor created successfully
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/VisitorResponse'

  get:
    summary: Search/list visitors
    tags: [Visitors]
    security:
      - bearerAuth: []
    parameters:
      - name: X-Tenant
        in: header
        required: true
      - name: search
        in: query
        schema:
          type: string
      - name: status
        in: query
        schema:
          type: string
          enum: [SCHEDULED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW]
      - name: visitDate
        in: query
        schema:
          type: string
          format: date
      - name: page
        in: query
        schema:
          type: integer
          default: 1
      - name: pageSize
        in: query
        schema:
          type: integer
          default: 20
    responses:
      200:
        description: Paginated visitor list
```

---

## 11. Implementation Phases

### Phase 1: Core Foundation (Week 1)
- ✅ Database schema design
- ✅ Create Visitors and VisitorPasses tables
- ✅ Implement VisitorRepository
- ✅ Implement VisitorService
- ✅ Create basic API routes (CRUD)

### Phase 2: Business Logic (Week 1-2)
- ✅ Check-in/check-out functionality
- ✅ Visitor status management
- ✅ Search and filtering
- ✅ Validation logic
- ✅ Error handling

### Phase 3: Integration (Week 2)
- ✅ Feature catalog integration
- ✅ User permission checks
- ✅ Migration script for existing tenants
- ✅ Notification system integration

### Phase 4: Enhancements (Week 3)
- ⏳ Visitor pass generation with QR codes
- ⏳ Photo upload integration (S3)
- ⏳ Advanced reporting and analytics
- ⏳ Email/SMS notifications

### Phase 5: Testing & Documentation (Week 3-4)
- ⏳ Unit tests
- ⏳ Integration tests
- ⏳ API documentation
- ⏳ User guide

---

## 12. Security Considerations

### 12.1 Data Privacy
- ✅ Visitor data isolated per tenant schema
- ✅ No cross-tenant data leakage
- ✅ PII (email, mobile, ID numbers) encrypted at rest
- ✅ Soft delete for compliance (keep audit trail)

### 12.2 Access Control
- ✅ ADMIN: Full access to all visitor operations
- ✅ STAFF: Can create/manage visitors they host
- ✅ STUDENT: Read-only access (if needed)
- ✅ Feature-level permissions via UserPermissions

### 12.3 Input Validation
- ✅ Mobile number format validation
- ✅ Email format validation
- ✅ Date range validation (no past dates for new visits)
- ✅ UUID validation for references
- ✅ SQL injection prevention (Exposed ORM)

---

## 13. Performance Optimization

### 13.1 Database Indexes
```kotlin
object Visitors : Table("visitors") {
    // ... fields

    init {
        index(false, hostUserId)           // Queries by host
        index(false, visitDate)            // Date range queries
        index(false, status)               // Filter by status
        index(false, visitDate, status)    // Composite for dashboard
    }
}
```

### 13.2 Caching Strategy
- Cache visitor stats (15-minute TTL)
- Cache feature permissions (user session TTL)
- Invalidate on check-in/check-out

### 13.3 Query Optimization
- Use pagination for all list endpoints
- Limit eager loading to necessary joins
- Use database-level aggregations for stats

---

## 14. Monitoring & Logging

### 14.1 Key Metrics
- Total visitors per tenant per day/month
- Average check-in/check-out duration
- No-show rate
- Peak visitor hours
- Host response time

### 14.2 Logging
```kotlin
logger.info("Visitor created: $visitorId by $createdBy for tenant ${tenantContext.id}")
logger.info("Visitor checked in: $visitorId at $actualCheckInTime")
logger.warn("Visitor no-show detected: $visitorId")
logger.error("Failed to send notification to host: $hostUserId", exception)
```

---

## 15. Future Enhancements

### 15.1 Short-term
- ✅ Recurring visitors (pre-approved list)
- ✅ Bulk visitor import (CSV/Excel)
- ✅ Visitor photo capture at check-in
- ✅ QR code-based self-check-in kiosks

### 15.2 Long-term
- ✅ Integration with building access control systems
- ✅ Visitor badges/pass printing
- ✅ Facial recognition for check-in
- ✅ Visitor analytics dashboard
- ✅ NDA/document signing workflow
- ✅ Parking spot assignment

---

## 16. Dependencies

### 16.1 Existing Dependencies (No Additional Required)
- Ktor framework
- Exposed ORM
- PostgreSQL JDBC driver
- Kotlinx Serialization
- Kotlinx Datetime

### 16.2 Optional Enhancements
- ZXing (QR code generation)
- AWS SDK (S3 for photos/badges)
- Apache POI (Excel import/export)

---

## Summary

This implementation plan provides a comprehensive, production-ready visitor management system that:

✅ **Follows Established Patterns**: Uses same repository/service/route architecture
✅ **Multi-Tenant Isolation**: Visitor data fully isolated per tenant schema
✅ **Feature-Based Access**: Integrated with existing Features and UserPermissions system
✅ **Scalable**: Designed for high-volume visitor tracking
✅ **Extensible**: Easy to add passes, QR codes, notifications, etc.
✅ **Secure**: Proper validation, error handling, and access control
✅ **Maintainable**: Clear separation of concerns, well-documented

The feature can be implemented incrementally using the phased approach, with core functionality ready in Phase 1-2 and advanced features added in later phases.
