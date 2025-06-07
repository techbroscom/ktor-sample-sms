package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.CreateClassRequest
import com.example.models.dto.UpdateClassRequest
import com.example.models.responses.ApiResponse
import com.example.services.ClassService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.classRoutes(classService: ClassService) {
    route("/api/v1/classes") {

        // Get all classes
        get {
            val classes = classService.getAllClasses()
            call.respond(ApiResponse(
                success = true,
                data = classes
            ))
        }

        // Get classes for active academic year
        get("/active") {
            val classes = classService.getClassesForActiveAcademicYear()
            call.respond(ApiResponse(
                success = true,
                data = classes
            ))
        }

        // Get classes by academic year (keep for backward compatibility)
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classes = classService.getClassesByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classes
            ))
        }

        // Get classes by name and section (all academic years)
        get("/search") {
            val className = call.request.queryParameters["className"]
                ?: throw ApiException("className parameter is required", HttpStatusCode.BadRequest)
            val sectionName = call.request.queryParameters["sectionName"]
                ?: throw ApiException("sectionName parameter is required", HttpStatusCode.BadRequest)

            val classes = classService.getClassesByNameAndSection(className, sectionName)
            call.respond(ApiResponse(
                success = true,
                data = classes
            ))
        }

        // Get classes by name and section for active academic year
        get("/search/active") {
            val className = call.request.queryParameters["className"]
                ?: throw ApiException("className parameter is required", HttpStatusCode.BadRequest)
            val sectionName = call.request.queryParameters["sectionName"]
                ?: throw ApiException("sectionName parameter is required", HttpStatusCode.BadRequest)

            val classes = classService.getClassesByNameAndSectionForActiveYear(className, sectionName)
            call.respond(ApiResponse(
                success = true,
                data = classes
            ))
        }

        // Get class by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val classDto = classService.getClassById(id)
            call.respond(ApiResponse(
                success = true,
                data = classDto
            ))
        }

        // Create class (uses active academic year if not specified)
        post {
            val request = call.receive<CreateClassRequest>()
            val classDto = classService.createClass(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = classDto,
                message = "Class created successfully"
            ))
        }

        // Update class (uses active academic year if not specified)
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateClassRequest>()
            val classDto = classService.updateClass(id, request)
            call.respond(ApiResponse(
                success = true,
                data = classDto,
                message = "Class updated successfully"
            ))
        }

        // Delete class
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            classService.deleteClass(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Class deleted successfully"
            ))
        }
    }
}