package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateFeesStructureRequest
import com.example.models.dto.CreateFeesStructureRequest
import com.example.models.dto.UpdateFeesStructureRequest
import com.example.models.responses.ApiResponse
import com.example.services.FeesStructureService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.feesStructureRoutes(feesStructureService: FeesStructureService) {
    route("/api/v1/fees-structures") {

        // Get all fees structures
        get {
            val feesStructures = feesStructureService.getAllFeesStructures()
            call.respond(ApiResponse(
                success = true,
                data = feesStructures
            ))
        }

        // Get fees structures by academic year
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val feesStructures = feesStructureService.getFeesByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = feesStructures
            ))
        }

        // Get fees structures by class
        get("/class/{classId}") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val feesStructures = feesStructureService.getFeesByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = feesStructures
            ))
        }

        // Get fees structures by class and academic year
        get("/class/{classId}/academic-year/{academicYearId}") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val feesStructures = feesStructureService.getFeesByClassAndAcademicYear(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = feesStructures
            ))
        }

        // Get mandatory fees by class and academic year
        get("/class/{classId}/academic-year/{academicYearId}/mandatory") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val mandatoryFees = feesStructureService.getMandatoryFees(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = mandatoryFees
            ))
        }

        // Get optional fees by class and academic year
        get("/class/{classId}/academic-year/{academicYearId}/optional") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val optionalFees = feesStructureService.getOptionalFees(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = optionalFees
            ))
        }

        // Get class fees structures summary for an academic year
        get("/academic-year/{academicYearId}/class-summary") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classSummaries = feesStructureService.getClassFeesStructures(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classSummaries
            ))
        }

        // Get fees structure summary for an academic year
        get("/academic-year/{academicYearId}/summary") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val summary = feesStructureService.getFeesStructureSummary(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = summary
            ))
        }

        // Create a new fees structure
        post {
            val request = call.receive<CreateFeesStructureRequest>()
            val feesStructure = feesStructureService.createFeesStructure(request)
            call.respond(
                status = HttpStatusCode.Created,
                message = ApiResponse(
                    success = true,
                    data = feesStructure,
                    message = "Fees structure created successfully"
                )
            )
        }

        // Bulk create fees structures
        post("/bulk") {
            val request = call.receive<BulkCreateFeesStructureRequest>()
            val feesStructures = feesStructureService.bulkCreateFeesStructures(request)
            call.respond(
                status = HttpStatusCode.Created,
                message = ApiResponse(
                    success = true,
                    data = feesStructures,
                    message = "${feesStructures.size} fees structures created successfully"
                )
            )
        }

        // Get fees structure by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Fees Structure ID is required", HttpStatusCode.BadRequest)

            val feesStructure = feesStructureService.getFeesStructureById(id)
            call.respond(ApiResponse(
                success = true,
                data = feesStructure
            ))
        }

        // Update fees structure
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Fees Structure ID is required", HttpStatusCode.BadRequest)
            val request = call.receive<UpdateFeesStructureRequest>()

            val feesStructure = feesStructureService.updateFeesStructure(id, request)
            call.respond(ApiResponse(
                success = true,
                data = feesStructure,
                message = "Fees structure updated successfully"
            ))
        }

        // Delete fees structure
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Fees Structure ID is required", HttpStatusCode.BadRequest)

            feesStructureService.deleteFeesStructure(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Fees structure deleted successfully"
            ))
        }

        // Delete all fees structures for a class
        delete("/class/{classId}") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = feesStructureService.removeAllFeesFromClass(classId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "$deletedCount fees structures deleted successfully"
            ))
        }
    }
}