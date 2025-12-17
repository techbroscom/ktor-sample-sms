package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.ExamService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.examRoutes(examService: ExamService) {
    route("/api/v1/exams") {

        // Get all exams
        get {
            val exams = examService.getAllExams()
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        get("/complete") {
            val exams = examService.getExamsByNameGrouped()
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        get("/examsByName") {
            val exams = examService.getExamsName()
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        get("/classByExamName/{examName}") {
            val examName = call.getPathParameter("examName", "Exam Name")
            val exams = examService.getExamsClassesName(examName)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        get("/examsByClassAndExamName/{classId}/{examName}") {
            val classId = call.getPathParameter("classId", "Class Id")
            val examName = call.getPathParameter("examName", "Exam Name")
            val exams = examService.getExamsByClassesAndExamsName(classId, examName)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        // Get exams by academic year
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val exams = examService.getExamsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        // Get exams by academic year
        get("/academic-year/active") {
            val exams = examService.getExamsByActiveAcademicYear()
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        // Get exams by class
        get("/class/{classId}") {
            val classId = call.getPathParameter("classId", "Class ID")
            val exams = examService.getExamsByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        // Get exams by subject
        get("/subject/{subjectId}") {
            val subjectId = call.getPathParameter("subjectId", "Subject ID")
            val exams = examService.getExamsBySubject(subjectId)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        // Get exams by class and subject
        get("/class/{classId}/subject/{subjectId}") {
            val classId = call.getPathParameter("classId", "Class ID")
            val subjectId = call.getPathParameter("subjectId", "Subject ID")

            val exams = examService.getExamsByClassAndSubject(classId, subjectId)

            call.respond(
                ApiResponse(
                    success = true,
                    data = exams
                )
            )
        }

        // Get exams by class and academic year
        get("/class/{classId}/academic-year/{academicYearId}") {
            val classId = call.getPathParameter("classId", "Class ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val exams = examService.getExamsByClassAndAcademicYear(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        // Get exams by subject and academic year
        get("/subject/{subjectId}/academic-year/{academicYearId}") {
            val subjectId = call.getPathParameter("subjectId", "Subject ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val exams = examService.getExamsBySubjectAndAcademicYear(subjectId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        post("/{id}/publish-results") {
            val examId = call.getPathParameter("id", "Exam ID")

            examService.publishResults(examId)

            call.respond(ApiResponse<Unit>(
                    success = true,
                    message = "Results published successfully"
            ))
        }

        post("/publish/by-exam-name/{examName}/class/{classId}") {
            val examName = call.getPathParameter("examName", "Exam Name")
            val classId = call.getPathParameter("classId", "Class ID")

            examService.publishResultsByExamNameAndClassId(classId, examName)

            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Results published successfully"
            ))
        }

        post("/publish/by-exam-name/{examName}") {
            val examName = call.getPathParameter("examName", "Exam Name")

            examService.publishResultsByExamName(examName)

            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Results published successfully"
            ))
        }


        post("/{id}/results-ready") {
            val examId = call.getPathParameter("id", "Exam ID")

            examService.markResultsReady(examId)

            call.respond(
                ApiResponse<Unit>(
                    success = true,
                    message = "Results marked as READY"
                )
            )
        }


        // Get exams by date
        /*get("/date/{date}") {
            val date = call.getPathParameter("date", "Date")
            val exams = examService.getExamsByDate(date)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }*/

        // Get exams by date range
        get("/date-range") {
            val startDate = call.request.queryParameters["startDate"]
                ?: throw ApiException("Start date is required", HttpStatusCode.BadRequest)
            val endDate = call.request.queryParameters["endDate"]
                ?: throw ApiException("End date is required", HttpStatusCode.BadRequest)
            val academicYearId = call.request.queryParameters["academicYearId"]

            val exams = examService.getExamsByDateRange(startDate, endDate, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = exams
            ))
        }

        // ================================
        // GROUPED DATA ENDPOINTS
        // ================================

        // Get exams grouped by class for an academic year
        get("/academic-year/{academicYearId}/grouped-by-class") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val groupedExams = examService.getExamsByClass(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = groupedExams
            ))
        }

        // Get exams grouped by subject for an academic year
        get("/academic-year/{academicYearId}/grouped-by-subject") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val groupedExams = examService.getExamsBySubject(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = groupedExams
            ))
        }

        // Get exams grouped by date for an academic year
        /*get("/academic-year/{academicYearId}/grouped-by-date") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val groupedExams = examService.getExamsByDate(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = groupedExams
            ))
        }*/

        // ================================
        // ANALYTICS AND SUMMARY ENDPOINTS
        // ================================

        // Get exam statistics for an academic year
        get("/academic-year/{academicYearId}/statistics") {
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val exams = examService.getExamsByAcademicYear(academicYearId)

            val statistics = mapOf(
                "totalExams" to exams.size,
                "examsByClass" to exams.groupBy { it.className }.mapValues { it.value.size },
                "examsBySubject" to exams.groupBy { it.subjectName }.mapValues { it.value.size },
                "examsByMonth" to exams.groupBy { it.date.substring(0, 7) }.mapValues { it.value.size },
                "averageMaxMarks" to if (exams.isNotEmpty()) exams.map { it.maxMarks }.average() else 0.0,
                "totalMaxMarks" to exams.sumOf { it.maxMarks },
                "upcomingExams" to exams.filter { it.date >= java.time.LocalDate.now().toString() }.size,
                "pastExams" to exams.filter { it.date < java.time.LocalDate.now().toString() }.size
            )

            call.respond(ApiResponse(
                success = true,
                data = statistics
            ))
        }

        // Get exam schedule for a specific class
        get("/class/{classId}/academic-year/{academicYearId}/schedule") {
            val classId = call.getPathParameter("classId", "Class ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val exams = examService.getExamsByClassAndAcademicYear(classId, academicYearId)

            val schedule = mapOf(
                "classId" to classId,
                "academicYearId" to academicYearId,
                "className" to exams.firstOrNull()?.className,
                "sectionName" to exams.firstOrNull()?.sectionName,
                "totalExams" to exams.size,
                "exams" to exams.sortedBy { it.date },
                "generatedAt" to System.currentTimeMillis()
            )

            call.respond(ApiResponse(
                success = true,
                data = schedule
            ))
        }

        // Get upcoming exams (next 30 days)
        get("/upcoming") {
            val academicYearId = call.request.queryParameters["academicYearId"]
            val today = java.time.LocalDate.now()
            val endDate = today.plusDays(30)

            val upcomingExams = examService.getExamsByDateRange(
                today.toString(),
                endDate.toString(),
                academicYearId
            )

            call.respond(ApiResponse(
                success = true,
                data = mapOf(
                    "dateRange" to mapOf(
                        "from" to today.toString(),
                        "to" to endDate.toString()
                    ),
                    "totalUpcomingExams" to upcomingExams.size,
                    "exams" to upcomingExams
                )
            ))
        }

        // Get exam calendar for a month
        get("/calendar") {
            val year = call.request.queryParameters["year"]?.toIntOrNull()
                ?: java.time.LocalDate.now().year
            val month = call.request.queryParameters["month"]?.toIntOrNull()
                ?: java.time.LocalDate.now().monthValue
            val academicYearId = call.request.queryParameters["academicYearId"]

            val startDate = java.time.LocalDate.of(year, month, 1)
            val endDate = startDate.withDayOfMonth(startDate.lengthOfMonth())

            val exams = examService.getExamsByDateRange(
                startDate.toString(),
                endDate.toString(),
                academicYearId
            )

            val calendar = mapOf(
                "year" to year,
                "month" to month,
                "monthName" to startDate.month.name,
                "totalExams" to exams.size,
                "examsByDate" to exams.groupBy { it.date },
                "examDates" to exams.map { it.date }.distinct().sorted()
            )

            call.respond(ApiResponse(
                success = true,
                data = calendar
            ))
        }

        // Validate exam scheduling (check for conflicts)
        get("/validate-schedule") {
            val classId = call.request.queryParameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val date = call.request.queryParameters["date"]
                ?: throw ApiException("Date is required", HttpStatusCode.BadRequest)
            val academicYearId = call.request.queryParameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)
            val excludeExamId = call.request.queryParameters["excludeExamId"]

            val existingExams = examService.getExamsByClassAndAcademicYear(classId, academicYearId)
                .filter { it.date == date }
                .let { exams ->
                    if (excludeExamId != null) {
                        exams.filter { it.id != excludeExamId }
                    } else {
                        exams
                    }
                }

            val hasConflict = existingExams.isNotEmpty()

            call.respond(ApiResponse(
                success = true,
                data = mapOf(
                    "hasConflict" to hasConflict,
                    "conflictingExams" to existingExams,
                    "classId" to classId,
                    "date" to date,
                    "academicYearId" to academicYearId
                )
            ))
        }

        // Get exam workload analysis for a class
        get("/class/{classId}/academic-year/{academicYearId}/workload-analysis") {
            val classId = call.getPathParameter("classId", "Class ID")
            val academicYearId = call.getPathParameter("academicYearId", "Academic Year ID")
            val exams = examService.getExamsByClassAndAcademicYear(classId, academicYearId)

            val workloadAnalysis = mapOf(
                "classId" to classId,
                "academicYearId" to academicYearId,
                "totalExams" to exams.size,
                "totalMaxMarks" to exams.sumOf { it.maxMarks },
                "averageMarksPerExam" to if (exams.isNotEmpty()) exams.map { it.maxMarks }.average() else 0.0,
                "examsByMonth" to exams.groupBy { it.date.substring(0, 7) }
                    .mapValues { (_, monthExams) ->
                        mapOf(
                            "count" to monthExams.size,
                            "totalMarks" to monthExams.sumOf { it.maxMarks },
                            "subjects" to monthExams.map { it.subjectName }.distinct()
                        )
                    },
                "heaviestWeeks" to exams.groupBy {
                    java.time.LocalDate.parse(it.date).let { date ->
                        "${date.year}-W${date.get(java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfYear())}"
                    }
                }.mapValues { (_, weekExams) ->
                    mapOf(
                        "count" to weekExams.size,
                        "totalMarks" to weekExams.sumOf { it.maxMarks },
                        "dates" to weekExams.map { it.date }.distinct().sorted()
                    )
                }.toList().sortedByDescending { it.second["count"] as Int }.take(5).toMap()
            )

            call.respond(ApiResponse(
                success = true,
                data = workloadAnalysis
            ))
        }

        // Bulk operations endpoint
        post("/bulk-operations") {
            val operation = call.request.queryParameters["operation"]
                ?: throw ApiException("Operation type is required", HttpStatusCode.BadRequest)

            when (operation.lowercase()) {
                "bulk-create" -> {
                    val request = call.receive<BulkCreateExamRequest>()
                    val exams = examService.bulkCreateExams(request)
                    call.respond(HttpStatusCode.Created, ApiResponse(
                        success = true,
                        message = "${exams.size} exams created successfully",
                        data = exams
                    ))
                }
                "duplicate-exams" -> {
                    val sourceAcademicYearId = call.request.queryParameters["sourceAcademicYearId"]
                        ?: throw ApiException("Source Academic Year ID is required", HttpStatusCode.BadRequest)
                    val targetAcademicYearId = call.request.queryParameters["targetAcademicYearId"]
                        ?: throw ApiException("Target Academic Year ID is required", HttpStatusCode.BadRequest)
                    val classId = call.request.queryParameters["classId"]

                    // This would require implementing a duplicate exam method in the service
                    // For now, return a placeholder response
                    call.respond(ApiResponse(
                        success = true,
                        message = "Exam duplication feature coming soon",
                        data = mapOf(
                            "sourceAcademicYearId" to sourceAcademicYearId,
                            "targetAcademicYearId" to targetAcademicYearId,
                            "classId" to classId
                        )
                    ))
                }
                else -> {
                    throw ApiException("Unsupported operation: $operation", HttpStatusCode.BadRequest)
                }
            }
        }

        // ================================
        // EXISTING CRUD ROUTES
        // ================================

        // Create single exam
        post {
            println("Create in Route Called")

            try {
                val request = call.receive<CreateExamRequest>()
                println(request.toString())

                val exam = examService.createExam(request)

                call.respond(
                    HttpStatusCode.Created,
                    ApiResponse(
                        success = true,
                        message = "Exam created successfully",
                        data = exam
                    )
                )
            } catch (e: ContentTransformationException) {
                // Handles invalid or malformed JSON in the request body
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse(
                        success = false,
                        message = "Invalid request format: ${e.message}",
                        data = null
                    )
                )
            } catch (e: Exception) {
                // Catches unexpected errors from service or elsewhere
                e.printStackTrace()
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse(
                        success = false,
                        message = "An unexpected error occurred: ${e.message}",
                        data = null
                    )
                )
            }
        }

        // Bulk create exams
        post("/bulk") {
            val request = call.receive<BulkCreateExamRequest>()
            val exams = examService.bulkCreateExams(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                message = "${exams.size} exams created successfully",
                data = exams
            ))
        }

        // Get specific exam
        get("/{id}") {
            val id = call.getPathParameter("id", "Exam ID")
            val exam = examService.getExamById(id)
            call.respond(ApiResponse(
                success = true,
                data = exam
            ))
        }

        // Update exam
        put("/{id}") {
            val id = call.getPathParameter("id", "Exam ID")
            val request = call.receive<UpdateExamRequest>()
            val exam = examService.updateExam(id, request)
            call.respond(ApiResponse(
                success = true,
                message = "Exam updated successfully",
                data = exam
            ))
        }

        // Delete exam
        delete("/{id}") {
            val id = call.getPathParameter("id", "Exam ID")
            examService.deleteExam(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Exam deleted successfully"
            ))
        }

        // Remove all exams from a class
        delete("/class/{classId}/all-exams") {
            val classId = call.getPathParameter("classId", "Class ID")
            val removedCount = examService.removeAllExamsFromClass(classId)
            call.respond(ApiResponse(
                success = true,
                message = "$removedCount exams removed from class",
                data = mapOf("removedCount" to removedCount)
            ))
        }

        // Remove all exams from a subject
        delete("/subject/{subjectId}/all-exams") {
            val subjectId = call.getPathParameter("subjectId", "Subject ID")
            val removedCount = examService.removeAllExamsFromSubject(subjectId)
            call.respond(ApiResponse(
                success = true,
                message = "$removedCount exams removed from subject",
                data = mapOf("removedCount" to removedCount)
            ))
        }
    }
}

// Extension function for cleaner parameter extraction
private fun ApplicationCall.getPathParameter(name: String, description: String): String {
    return parameters[name] ?: throw ApiException("$description is required", HttpStatusCode.BadRequest)
}