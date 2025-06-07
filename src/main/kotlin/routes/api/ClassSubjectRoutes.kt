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

        // Get class-subject assignments for active academic year
        get("/active") {
            val classSubjects = classSubjectService.getClassSubjectsForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get class-subject assignments by academic year (keep for backward compatibility)
        get("/academic-year/{academicYearId}") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getClassSubjectsByAcademicYear(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get subjects by class (all academic years)
        get("/class/{classId}/subjects") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getSubjectsByClass(classId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get subjects by class for active academic year
        get("/class/{classId}/subjects/active") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getSubjectsByClassForActiveYear(classId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get classes by subject (all academic years)
        get("/subject/{subjectId}/classes") {
            val subjectId = call.parameters["subjectId"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getClassesBySubject(subjectId)
            call.respond(ApiResponse(
                success = true,
                data = classSubjects
            ))
        }

        // Get classes by subject for active academic year
        get("/subject/{subjectId}/classes/active") {
            val subjectId = call.parameters["subjectId"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val classSubjects = classSubjectService.getClassesBySubjectForActiveYear(subjectId)
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

        // Get classes with their subjects for active academic year
        get("/active/classes-with-subjects") {
            val classesWithSubjects = classSubjectService.getClassesWithSubjectsForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = classesWithSubjects
            ))
        }

        // Get classes with their subjects for an academic year (keep for backward compatibility)
        get("/academic-year/{academicYearId}/classes-with-subjects") {
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classesWithSubjects = classSubjectService.getClassesWithSubjects(academicYearId)
            call.respond(ApiResponse(
                success = true,
                data = classesWithSubjects
            ))
        }

        // Get subjects with their classes for active academic year
        get("/active/subjects-with-classes") {
            val subjectsWithClasses = classSubjectService.getSubjectsWithClassesForActiveYear()
            call.respond(ApiResponse(
                success = true,
                data = subjectsWithClasses
            ))
        }

        // Get subjects with their classes for an academic year (keep for backward compatibility)
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

        // Create class-subject assignment (uses active academic year if not specified)
        post {
            val request = call.receive<CreateClassSubjectRequest>()
            val classSubject = classSubjectService.createClassSubject(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = classSubject,
                message = "Class-Subject assignment created successfully"
            ))
        }

        // Bulk create class-subject assignments (uses active academic year if not specified)
        post("/bulk") {
            val request = call.receive<BulkCreateClassSubjectRequest>()
            val classSubjects = classSubjectService.bulkCreateClassSubjects(request)
            call.respond(HttpStatusCode.Created, ApiResponse(
                success = true,
                data = classSubjects,
                message = "${classSubjects.size} Class-Subject assignments created successfully"
            ))
        }

        // Update class-subject assignment (uses active academic year if not specified)
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

        // Remove all subjects from a class (all academic years)
        delete("/class/{classId}/subjects") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = classSubjectService.removeAllSubjectsFromClass(classId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "$deletedCount subject assignments removed from class"
            ))
        }

        // Remove all subjects from a class for active academic year
        delete("/class/{classId}/subjects/active") {
            val classId = call.parameters["classId"]
                ?: throw ApiException("Class ID is required", HttpStatusCode.BadRequest)

            val deletedCount = classSubjectService.removeAllSubjectsFromClassForActiveYear(classId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "$deletedCount subject assignments removed from class for active academic year"
            ))
        }

        // Remove a subject from all classes (all academic years)
        delete("/subject/{subjectId}/classes") {
            val subjectId = call.parameters["subjectId"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val deletedCount = classSubjectService.removeClassFromAllSubjects(subjectId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Subject removed from $deletedCount classes"
            ))
        }

        // Remove a subject from all classes for active academic year
        delete("/subject/{subjectId}/classes/active") {
            val subjectId = call.parameters["subjectId"]
                ?: throw ApiException("Subject ID is required", HttpStatusCode.BadRequest)

            val deletedCount = classSubjectService.removeClassFromAllSubjectsForActiveYear(subjectId)
            call.respond(ApiResponse<Unit>(
                success = true,
                message = "Subject removed from $deletedCount classes for active academic year"
            ))
        }
    }
}