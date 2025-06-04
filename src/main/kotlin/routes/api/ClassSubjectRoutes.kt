package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.BulkCreateClassSubjectRequest
import com.example.models.dto.CreateClassSubjectRequest
import com.example.models.dto.UpdateClassSubjectRequest
import com.example.models.responses.ApiResponse
import com.example.services.ClassSubjectService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.classSubjectRoutes(classSubjectService: ClassSubjectService) {
    route("/api/v1/class-subjects") {

        // Get all class-subject assignments
        get {
            val classSubjects = classSubjectService.getAllClassSubjects()
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get class-subject assignments by academic year
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getClassSubjectsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get subjects by class
        get("/class/{classId}/subjects") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getSubjectsByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get classes by subject
        get("/subject/{subjectId}/classes") {
            val subjectId = call.parameters["subjectId"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getClassesBySubject(subjectId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get subjects by class and academic year
        get("/class/{classId}/academic-year/{academicYearId}/subjects") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getSubjectsByClassAndAcademicYear(classId, academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get classes with their subjects for an academic year
        get("/academic-year/{academicYearId}/classes-with-subjects") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classesWithSubjects = classSubjectService.getClassesWithSubjects(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classesWithSubjects
            ))
        }

        // Get subjects with their classes for an academic year
        get("/academic-year/{academicYearId}/subjects-with-classes") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val subjectsWithClasses = classSubjectService.getSubjectsWithClasses(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = subjectsWithClasses
            ))
        }

        // Get class-subject assignment by ID
        get("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Class Subject ID is required", HttpStatusCode.BadRequest)

            val classSubject = classSubjectService.getClassSubjectById(id)
            call.respond(ApiResponse(
                success = true,
                data = classSubject
            ))
        }

        // Create class-subject assignment
        post {
            val request = call.receive<CreateClassSubjectRequest>()
            val classSubject = classSubjectService.createClassSubject(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = classSubject,
                message = "Class-Subject assignment created successfully"
            ))
        }

        // Bulk create class-subject assignments
        post("/bulk") {
            val request = call.receive<BulkCreateClassSubjectRequest>()
            val classSubjects = classSubjectService.bulkCreateClassSubjects(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = classSubjects,
                message = "${classSubjects.size} Class-Subject assignments created successfully"
            ))
        }

        // Update class-subject assignment
        put("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Class Subject ID is required", HttpStatusCode.BadRequest)

            val request = call.receive<UpdateClassSubjectRequest>()
            val classSubject = classSubjectService.updateClassSubject(id, request)
            call.respond(ApiResponse(
                success = true,
                data = classSubject,
                message = "Class-Subject assignment updated successfully"
            ))
        }

        // Delete class-subject assignment
        delete("/{id}") {
            val id = call.parameters["id"]
                ?: throw ApiException("Class Subject ID is required", HttpStatusCode.BadRequest)

            classSubjectService.deleteClassSubject(id)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Class-Subject assignment deleted successfully"
            ))
        }

        // Remove all subjects from a class
        delete("/class/{classId}/subjects") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = classSubjectService.removeAllSubjectsFromClass(classId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "$deletedCount subject assignments removed from class"
            ))
        }

        // Remove a subject from all classes
        delete("/subject/{subjectId}/classes") {
            val subjectId = call.parameters["subjectId"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val deletedCount = classSubjectService.removeClassFromAllSubjects(subjectId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Subject removed from $deletedCount classes"
            ))
        }
    }
}