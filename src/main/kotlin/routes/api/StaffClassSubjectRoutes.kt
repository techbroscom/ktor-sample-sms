package com.example.routes.api

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.models.responses.ApiResponse
import com.example.services.StaffClassAssignmentService
import com.example.services.StaffSubjectAssignmentService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.staffClassSubjectRoutes(
    staffClassAssignmentService: StaffClassAssignmentService,
    staffSubjectAssignmentService: StaffSubjectAssignmentService
) {
    route("/api/v1/staff") {

        // Get staff's class and subject details
        get("/{staffId}/class-subject-details") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val classSubjectDetails = getStaffClassSubjectDetails(
                staffId,
                staffClassAssignmentService,
                staffSubjectAssignmentService
            )

            call.respond(ApiResponse(
                success = true,
                data = classSubjectDetails
            ))
        }

        // Get staff's class and subject details for active academic year
        get("/{staffId}/class-subject-details/active-year") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)

            val classSubjectDetails = getStaffClassSubjectDetailsForActiveYear(
                staffId,
                staffClassAssignmentService,
                staffSubjectAssignmentService
            )

            call.respond(ApiResponse(
                success = true,
                data = classSubjectDetails
            ))
        }

        // Get staff's class and subject details for specific academic year
        get("/{staffId}/academic-year/{academicYearId}/class-subject-details") {
            val staffId = call.parameters["staffId"]
                ?: throw ApiException("Staff ID is required", HttpStatusCode.BadRequest)
            val academicYearId = call.parameters["academicYearId"]
                ?: throw ApiException("Academic Year ID is required", HttpStatusCode.BadRequest)

            val classSubjectDetails = getStaffClassSubjectDetailsForAcademicYear(
                staffId,
                academicYearId,
                staffClassAssignmentService,
                staffSubjectAssignmentService
            )

            call.respond(ApiResponse(
                success = true,
                data = classSubjectDetails
            ))
        }
    }
}

suspend fun getStaffClassSubjectDetails(
    staffId: String,
    staffClassAssignmentService: StaffClassAssignmentService,
    staffSubjectAssignmentService: StaffSubjectAssignmentService
): StaffClassSubjectDetailsDto {

    // Get classes assigned to staff (homeroom/class teacher)
    val classAssignments = staffClassAssignmentService.getClassesByStaff(staffId)

    // Get subjects taught by staff
    val subjectAssignments = staffSubjectAssignmentService.getSubjectsByStaff(staffId)

    return buildStaffClassSubjectDetails(classAssignments, subjectAssignments)
}

suspend fun getStaffClassSubjectDetailsForActiveYear(
    staffId: String,
    staffClassAssignmentService: StaffClassAssignmentService,
    staffSubjectAssignmentService: StaffSubjectAssignmentService
): StaffClassSubjectDetailsDto {

    // Get classes assigned to staff for active year
    val classAssignments = staffClassAssignmentService.getClassesByStaffForActiveYear(staffId)

    // Get subjects taught by staff for active year
    val subjectAssignments = staffSubjectAssignmentService.getSubjectsByStaffForActiveYear(staffId)

    return buildStaffClassSubjectDetails(classAssignments, subjectAssignments)
}

suspend fun getStaffClassSubjectDetailsForAcademicYear(
    staffId: String,
    academicYearId: String,
    staffClassAssignmentService: StaffClassAssignmentService,
    staffSubjectAssignmentService: StaffSubjectAssignmentService
): StaffClassSubjectDetailsDto {

    // Get classes assigned to staff for specific academic year
    val classAssignments = staffClassAssignmentService.getClassesByStaffAndAcademicYear(staffId, academicYearId)

    // Get subjects taught by staff for specific academic year
    val subjectAssignments = staffSubjectAssignmentService.getSubjectsByStaffAndAcademicYear(staffId, academicYearId)

    return buildStaffClassSubjectDetails(classAssignments, subjectAssignments)
}

private fun buildStaffClassSubjectDetails(
    classAssignments: List<StaffClassAssignmentDto>,
    subjectAssignments: List<StaffSubjectAssignmentDto>
): StaffClassSubjectDetailsDto {

    // Map class assignments to myClass format
    val myClasses = classAssignments.map { assignment ->
        MyClassDto(
            id = assignment.classId,
            className = assignment.className,
            sectionName = assignment.sectionName,
            academicYearId = assignment.academicYearId,
            academicYearName = assignment.academicYearName
        )
    }

    // Map subject assignments to teachingClasses format
    val teachingClasses = subjectAssignments.map { assignment ->
        TeachingClassDto(
            id = assignment.classId,
            className = assignment.className,
            sectionName = assignment.sectionName,
            subjectName = assignment.subjectName,
            subjectId = assignment.classSubjectId
        )
    }

    return StaffClassSubjectDetailsDto(
        myClass = myClasses,
        teachingClasses = teachingClasses
    )
}