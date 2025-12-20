# Library Management & Future Features Plan

## 📚 Library Management System - Detailed Plan

### Overview
A comprehensive library management system for tracking books, borrowing/returns, fines, and reservations in a multi-tenant school environment.

---

## 1. Library Management - Database Schema

### 1.1 Books Table (Tenant Schema)
```kotlin
object Books : Table("books") {
    val id = uuid("id")
    val isbn = varchar("isbn", 20).nullable()
    val title = varchar("title", 255)
    val author = varchar("author", 255)
    val publisher = varchar("publisher", 255).nullable()
    val publicationYear = integer("publication_year").nullable()
    val edition = varchar("edition", 50).nullable()
    val language = varchar("language", 50).default("English")
    val category = varchar("category", 100)  // Fiction, Non-Fiction, Reference, etc.
    val subCategory = varchar("sub_category", 100).nullable()
    val totalCopies = integer("total_copies").default(1)
    val availableCopies = integer("available_copies").default(1)
    val shelfLocation = varchar("shelf_location", 50).nullable()
    val coverImageUrl = varchar("cover_image_url", 500).nullable()
    val description = text("description").nullable()
    val price = decimal("price", 10, 2).nullable()
    val status = enumerationByName("status", 20, BookStatus::class)  // AVAILABLE, LOST, DAMAGED, MAINTENANCE
    val addedBy = uuid("added_by").references(Users.id)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, isbn)
        index(false, category)
        index(false, author)
        index(false, title)
    }
}

enum class BookStatus {
    AVAILABLE,
    OUT_OF_STOCK,
    LOST,
    DAMAGED,
    MAINTENANCE
}
```

### 1.2 BookBorrowings Table (Tenant Schema)
```kotlin
object BookBorrowings : Table("book_borrowings") {
    val id = uuid("id")
    val bookId = uuid("book_id").references(Books.id)
    val userId = uuid("user_id").references(Users.id)  // Student/Staff borrowing
    val borrowedDate = datetime("borrowed_date").default(LocalDateTime.now())
    val dueDate = datetime("due_date")
    val returnedDate = datetime("returned_date").nullable()
    val status = enumerationByName("status", 20, BorrowingStatus::class)
    val renewalCount = integer("renewal_count").default(0)
    val issuedBy = uuid("issued_by").references(Users.id)  // Librarian
    val returnedTo = uuid("returned_to").references(Users.id).nullable()
    val condition = varchar("condition", 50).nullable()  // Good, Fair, Damaged
    val notes = text("notes").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, bookId)
        index(false, userId)
        index(false, status)
        index(false, dueDate)
    }
}

enum class BorrowingStatus {
    ACTIVE,      // Currently borrowed
    RETURNED,    // Returned on time
    OVERDUE,     // Past due date, not returned
    LOST,        // Book declared lost
    DAMAGED      // Book returned damaged
}
```

### 1.3 BookReservations Table (Tenant Schema)
```kotlin
object BookReservations : Table("book_reservations") {
    val id = uuid("id")
    val bookId = uuid("book_id").references(Books.id)
    val userId = uuid("user_id").references(Users.id)
    val reservedDate = datetime("reserved_date").default(LocalDateTime.now())
    val expiryDate = datetime("expiry_date")
    val status = enumerationByName("status", 20, ReservationStatus::class)
    val notified = bool("notified").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, bookId)
        index(false, userId)
        index(false, status)
    }
}

enum class ReservationStatus {
    PENDING,     // Waiting for book to be available
    AVAILABLE,   // Book is ready for pickup
    FULFILLED,   // User picked up the book
    EXPIRED,     // Reservation expired
    CANCELLED    // User cancelled
}
```

### 1.4 LibraryFines Table (Tenant Schema)
```kotlin
object LibraryFines : Table("library_fines") {
    val id = uuid("id")
    val borrowingId = uuid("borrowing_id").references(BookBorrowings.id)
    val userId = uuid("user_id").references(Users.id)
    val fineType = enumerationByName("fine_type", 20, FineType::class)
    val amount = decimal("amount", 10, 2)
    val reason = varchar("reason", 500)
    val daysOverdue = integer("days_overdue").nullable()
    val status = enumerationByName("status", 20, FineStatus::class)
    val paidAmount = decimal("paid_amount", 10, 2).default(BigDecimal.ZERO)
    val paidDate = datetime("paid_date").nullable()
    val paidTo = uuid("paid_to").references(Users.id).nullable()
    val waived = bool("waived").default(false)
    val waivedBy = uuid("waived_by").references(Users.id).nullable()
    val waivedReason = varchar("waived_reason", 500).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, userId)
        index(false, status)
    }
}

enum class FineType {
    OVERDUE,     // Late return
    LOST_BOOK,   // Book declared lost
    DAMAGE       // Book returned damaged
}

enum class FineStatus {
    PENDING,
    PARTIALLY_PAID,
    PAID,
    WAIVED
}
```

### 1.5 LibrarySettings Table (Tenant Schema)
```kotlin
object LibrarySettings : Table("library_settings") {
    val id = integer("id").autoIncrement()
    val maxBooksPerStudent = integer("max_books_per_student").default(3)
    val maxBooksPerStaff = integer("max_books_per_staff").default(5)
    val borrowingPeriodDays = integer("borrowing_period_days").default(14)
    val maxRenewals = integer("max_renewals").default(2)
    val overdueFinePer Day = decimal("overdue_fine_per_day", 10, 2).default(BigDecimal("5.00"))
    val lostBookFineMultiplier = decimal("lost_book_fine_multiplier", 5, 2).default(BigDecimal("2.00"))
    val reservationExpiryDays = integer("reservation_expiry_days").default(3)
    val enableReservations = bool("enable_reservations").default(true)
    val enableFines = bool("enable_fines").default(true)
    val updatedAt = datetime("updated_at").nullable()
    val updatedBy = uuid("updated_by").references(Users.id).nullable()

    override val primaryKey = PrimaryKey(id)
}
```

---

## 2. API Endpoints

```
Books Management:
POST   /api/v1/library/books                  - Add new book
GET    /api/v1/library/books                  - Search/list books
GET    /api/v1/library/books/:id              - Get book details
PUT    /api/v1/library/books/:id              - Update book
DELETE /api/v1/library/books/:id              - Delete book
GET    /api/v1/library/books/:id/availability - Check availability
GET    /api/v1/library/books/categories       - Get all categories

Borrowing:
POST   /api/v1/library/borrowings             - Issue book to user
POST   /api/v1/library/borrowings/:id/return  - Return book
POST   /api/v1/library/borrowings/:id/renew   - Renew borrowing
GET    /api/v1/library/borrowings             - List all borrowings
GET    /api/v1/library/borrowings/active      - Active borrowings
GET    /api/v1/library/borrowings/overdue     - Overdue borrowings
GET    /api/v1/library/borrowings/user/:id    - User's borrowing history

Reservations:
POST   /api/v1/library/reservations           - Reserve a book
GET    /api/v1/library/reservations           - List reservations
GET    /api/v1/library/reservations/:id       - Get reservation
PUT    /api/v1/library/reservations/:id       - Update reservation (fulfill/cancel)
DELETE /api/v1/library/reservations/:id       - Cancel reservation

Fines:
GET    /api/v1/library/fines                  - List all fines
GET    /api/v1/library/fines/user/:id         - User's fines
POST   /api/v1/library/fines/:id/pay          - Pay fine
POST   /api/v1/library/fines/:id/waive        - Waive fine

Settings:
GET    /api/v1/library/settings               - Get library settings
PUT    /api/v1/library/settings               - Update settings

Dashboard:
GET    /api/v1/library/dashboard              - Library statistics
GET    /api/v1/library/reports/popular-books  - Most borrowed books
GET    /api/v1/library/reports/user-stats     - User borrowing statistics
```

---

## 3. Key Features

### Automatic Overdue Detection
- Scheduled job to check for overdue books daily
- Automatically update status to OVERDUE
- Calculate and create fines based on settings

### Notification System
- Email/SMS when book is due in 2 days
- Overdue notifications
- Reservation availability notifications
- Fine payment reminders

### Advanced Search
- Search by title, author, ISBN, category
- Filter by availability, language, year
- Sorting by popularity, date added, title

### Reports & Analytics
- Most popular books
- Active borrowers
- Overdue report
- Revenue from fines
- Inventory status

---

## 4. Business Rules

1. **Borrowing Limits**: Students can borrow max 3 books, Staff max 5
2. **Borrowing Period**: Default 14 days, configurable per tenant
3. **Renewals**: Max 2 renewals per book, only if no reservations
4. **Reservations**: Auto-expire after 3 days if not picked up
5. **Fines**:
   - ₹5/day for overdue (configurable)
   - 2x book price for lost books
   - Damage fines assessed by librarian
6. **Cannot Borrow**: If user has pending fines > threshold or overdue books

---

# 🚀 Other Recommended Features

## Feature Priority Matrix

### High Priority (Immediate Value)

#### 1. **Hostel/Dormitory Management** 🏠
**Value**: Essential for boarding schools

**Database Schema:**
- `Hostels` - Hostel buildings
- `Rooms` - Room details with capacity
- `RoomAllocations` - Student room assignments
- `HostelAttendance` - Daily attendance
- `LeaveRequests` - Students requesting leave
- `HostelFees` - Room rent, mess fees
- `Complaints` - Hostel-related complaints

**Features:**
- Room allocation and management
- Hostel attendance tracking
- Leave request management
- Mess management
- Hostel fees integration with existing fees module
- Visitor management for hostel (reuse visitor management!)
- Complaint management

**API Endpoints:**
```
POST   /api/v1/hostels
GET    /api/v1/hostels
POST   /api/v1/hostels/:id/rooms
GET    /api/v1/hostels/:id/rooms
POST   /api/v1/hostels/allocations
GET    /api/v1/hostels/allocations
POST   /api/v1/hostels/attendance
GET    /api/v1/hostels/leave-requests
POST   /api/v1/hostels/leave-requests
```

---

#### 2. **Events & Activities Management** 🎭
**Value**: Track school events, competitions, extracurricular activities

**Database Schema:**
- `Events` - School events (sports day, annual function, etc.)
- `EventCategories` - Sports, Cultural, Academic, etc.
- `EventParticipants` - Student/staff registrations
- `EventResults` - Winners, scores
- `Announcements` - Event announcements

**Features:**
- Event creation and scheduling
- Student/staff registration
- Attendance tracking
- Results and awards management
- Photo gallery integration (S3)
- Calendar integration

**API Endpoints:**
```
POST   /api/v1/events
GET    /api/v1/events
GET    /api/v1/events/upcoming
POST   /api/v1/events/:id/register
POST   /api/v1/events/:id/results
GET    /api/v1/events/calendar
```

---

#### 3. **Parent-Teacher Communication** 👨‍👩‍👧‍👦
**Value**: Essential for parent engagement

**Database Schema:**
- `ParentAccounts` - Parent login credentials
- `StudentParents` - Link students to parents
- `Messages` - P-T communication
- `Meetings` - Schedule parent-teacher meetings
- `FeedbackForms` - Parent feedback

**Features:**
- Parent portal access
- View student performance
- Direct messaging with teachers
- Schedule meetings
- Approve leave requests
- View attendance, fees, exam results
- Receive notifications

**API Endpoints:**
```
POST   /api/v1/parents
GET    /api/v1/parents/students/:studentId
GET    /api/v1/parents/messages
POST   /api/v1/parents/messages
POST   /api/v1/parents/meetings/request
GET    /api/v1/parents/student/:id/performance
```

---

#### 4. **Inventory Management** 📦
**Value**: Track school assets, supplies, equipment

**Database Schema:**
- `InventoryCategories` - Furniture, Electronics, Sports, Stationery
- `InventoryItems` - All assets
- `ItemAllocations` - Assigned to classes/departments
- `MaintenanceRecords` - Repair history
- `PurchaseOrders` - Procurement tracking
- `Vendors` - Supplier information

**Features:**
- Asset tracking with barcodes/QR codes
- Stock management
- Allocation to departments/classes
- Maintenance scheduling
- Purchase order management
- Low stock alerts
- Depreciation tracking

**API Endpoints:**
```
POST   /api/v1/inventory/items
GET    /api/v1/inventory/items
GET    /api/v1/inventory/low-stock
POST   /api/v1/inventory/allocate
POST   /api/v1/inventory/maintenance
POST   /api/v1/inventory/purchase-orders
```

---

### Medium Priority (Strategic Value)

#### 5. **Online Learning / LMS** 💻
**Value**: Remote learning, recorded lectures, assignments

**Database Schema:**
- `Courses` - Online courses
- `CourseMaterials` - Videos, PDFs, links
- `Quizzes` - Online assessments
- `Submissions` - Student submissions
- `CourseProgress` - Tracking completion

**Features:**
- Upload course materials (videos, PDFs)
- Create quizzes and assessments
- Track student progress
- Discussion forums
- Live class integration (Zoom/Google Meet)
- Certificate generation

---

#### 6. **Staff Attendance & Payroll** 💼
**Value**: HR management

**Database Schema:**
- `StaffAttendance` - Daily attendance
- `LeaveTypes` - Casual, Sick, Earned
- `LeaveApplications` - Staff leave requests
- `Payroll` - Salary processing
- `Deductions` - Fines, advances
- `PaySlips` - Monthly pay slips

**Features:**
- Biometric integration
- Leave management
- Salary calculation
- Pay slip generation
- Tax calculations
- Attendance reports

---

#### 7. **Canteen/Cafeteria Management** 🍽️
**Value**: Meal planning, billing

**Database Schema:**
- `MenuItems` - Food items
- `DailyMenus` - Daily meal plans
- `CanteenOrders` - Student orders
- `CanteenBilling` - Monthly bills
- `CanteenInventory` - Food stock

**Features:**
- Menu planning
- Pre-ordering meals
- Billing and payments
- Nutrition tracking
- Inventory management
- Vendor management

---

#### 8. **Health & Medical Records** 🏥
**Value**: Student health tracking

**Database Schema:**
- `MedicalRecords` - Student health history
- `Vaccinations` - Immunization records
- `ClinicVisits` - Infirmary visits
- `Medications` - Prescribed medicines
- `Allergies` - Allergy information

**Features:**
- Health checkup records
- Vaccination tracking
- Emergency contact information
- Medication tracking
- Health reports

---

### Low Priority (Nice to Have)

#### 9. **Alumni Management** 🎓
- Alumni database
- Networking platform
- Job postings
- Donation management
- Events for alumni

#### 10. **Student Clubs & Societies** 🎪
- Club registration
- Membership management
- Activity tracking
- Budget management

#### 11. **Examination Hall Allocation** 📝
- Seat allocation
- Invigilator assignment
- Hall capacity management

#### 12. **Certificate Generation** 📜
- Transfer certificates
- Character certificates
- Bonafide certificates
- Marksheet generation

---

## Implementation Roadmap

### Phase 1: Core Operations (1-2 months)
1. ✅ Visitor Management (Completed)
2. 📚 Library Management
3. 🏠 Hostel Management
4. 📦 Inventory Management

### Phase 2: Communication & Engagement (2-3 months)
5. 👨‍👩‍👧‍👦 Parent-Teacher Communication
6. 🎭 Events & Activities
7. 📱 Enhanced Notifications

### Phase 3: Advanced Features (3-4 months)
8. 💻 Online Learning (LMS)
9. 💼 Staff Attendance & Payroll
10. 🍽️ Canteen Management
11. 🏥 Health Records

### Phase 4: Reporting & Analytics (Ongoing)
12. Advanced dashboards
13. Custom report builder
14. Data export capabilities
15. Predictive analytics

---

## Technology Considerations

### Integration Points
- **Existing Features**: Fees, Attendance, Users, Classes already exist
- **Reusable Components**:
  - File storage (S3) for documents
  - Notification system (FCM) for alerts
  - Feature management for tenant control
  - User permissions for access control

### Performance Optimizations
- Caching for frequently accessed data
- Pagination for large datasets
- Batch processing for reports
- Scheduled jobs for automation

### Security
- Multi-tenant data isolation
- Role-based access control
- Audit logging
- Data encryption

---

## Suggested Next Steps

1. **Library Management** (Most Requested)
   - Universal need across all schools
   - Clear ROI with fine management
   - Builds on existing architecture

2. **Hostel Management** (Boarding Schools)
   - Critical for residential schools
   - Reuses visitor management patterns
   - Integrates with fees module

3. **Parent Portal** (High Engagement)
   - Improves parent satisfaction
   - Reduces administrative burden
   - Increases system value

Which feature would you like to implement next? I can create a detailed implementation plan similar to visitor management! 🚀
