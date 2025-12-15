# ktor-sample

This project was created using the [Ktor Project Generator](https://start.ktor.io).

Here are some useful links to get you started:

- [Ktor Documentation](https://ktor.io/docs/home.html)
- [Ktor GitHub page](https://github.com/ktorio/ktor)
- The [Ktor Slack chat](https://app.slack.com/client/T09229ZC6/C0A974TJ9). You'll need to [request an invite](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up) to join.

## Features

Here's a list of features included in this project:

| Name                                                                   | Description                                                                        |
| ------------------------------------------------------------------------|------------------------------------------------------------------------------------ |
| [Routing](https://start.ktor.io/p/routing)                             | Provides a structured routing DSL                                                  |
| [kotlinx.serialization](https://start.ktor.io/p/kotlinx-serialization) | Handles JSON serialization using kotlinx.serialization library                     |
| [Content Negotiation](https://start.ktor.io/p/content-negotiation)     | Provides automatic content conversion according to Content-Type and Accept headers |
| [Exposed](https://start.ktor.io/p/exposed)                             | Adds Exposed database to your application                                          |
| [Call Logging](https://start.ktor.io/p/call-logging)                   | Logs client requests                                                               |

## Building & Running

To build or run the project, use one of the following tasks:

| Task                          | Description                                                          |
| -------------------------------|---------------------------------------------------------------------- |
| `./gradlew test`              | Run the tests                                                        |
| `./gradlew build`             | Build everything                                                     |
| `buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `buildImage`                  | Build the docker image to use with the fat JAR                       |
| `publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `run`                         | Run the server                                                       |
| `runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

## Java Version

This project targets **Java 11**. Install JDK 11 and either set `JAVA_HOME` to your JDK 11 installation or rely on Gradle's toolchain. Example (Windows PowerShell):

```powershell
# set JAVA_HOME for current session
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11'
./gradlew clean build
```

For deployment platforms that look for `system.properties` (e.g., Heroku), the project includes `system.properties` with `java.runtime.version=11`.

## API Documentation

The complete API is documented in **OpenAPI 3.0** format. The specification file is:

- [openapi.yaml](openapi.yaml) - Full API specification (150+ endpoints)

### View API Documentation

#### Option 1: Swagger UI Online
Visit [Swagger Editor](https://editor.swagger.io/) and paste the content of `openapi.yaml` to view interactive API documentation.

#### Option 2: Redoc (ReDoc) Online
1. Go to https://redocly.com/docs/redoc/deployment/
2. Paste the URL to `openapi.yaml` to view API documentation

#### Option 3: Local Swagger UI (Recommended)
Add the following dependency to `build.gradle.kts` and endpoint in your Ktor app:

```kotlin
// In build.gradle.kts dependencies
implementation("io.ktor:ktor-server-swagger:3.1.3")

// In Application.kt module function
import io.ktor.server.plugins.swagger.*

install(Swagger)

// Then access at: http://localhost:8080/swagger-ui
```

### API Overview

- **Base Path**: `/api/v1`
- **Total Endpoints**: 150+
- **Authentication**: Email/Password, OTP, FCM-based login
- **Response Format**: JSON with standardized ApiResponse wrapper

### Main Resource Categories

1. **Users & Authentication** - Login, registration, OTP, password management
2. **Academic Structure** - Classes, subjects, academic years
3. **Assignments** - Student and staff assignments
4. **Exams** - Exam management, schedules, results
5. **Attendance** - Attendance tracking
6. **Fees** - Fee structure management
7. **Posts & Announcements** - News and updates
8. **Complaints** - Complaint management
9. **Holidays** - Holiday calendar
10. **Files** - Profile pictures and document uploads
11. **Notifications** - FCM push notifications (personal and broadcast)
12. **School Configuration** - School settings
13. **Rules & Regulations** - School rules
14. **Dashboard** - Analytics and reports

## Running with a database

- By default the app attempts to use a database specified by `JDBC_DATABASE_URL` or `DATABASE_URL` environment variables (both JDBC and simple postgres:// styles are supported). Example:

```powershell
setx JDBC_DATABASE_URL "jdbc:postgresql://host:5432/dbname?user=user&password=pass"
setx DB_USER "user"
setx DB_PASSWORD "pass"
```

- If no database is configured or connection fails the app will fall back to an in-memory H2 database (useful for local development).

If you see startup errors mentioning PostgreSQL/SCRAM iteration (like "iteration must be >= 4096"), it's a server-side auth configuration incompatibility — using a local H2 for development or connecting to a different Postgres instance is the easiest workaround.

