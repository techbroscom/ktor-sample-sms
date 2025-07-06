package com.example.services

import com.example.exceptions.ApiException
import com.example.models.dto.*
import com.example.repositories.FeesStructureRepository
import io.ktor.http.*
import java.math.BigDecimal

class FeesStructureService(
    private val feesStructureRepository: FeesStructureRepository,
    private val classService: ClassService,
    private val academicYearService: AcademicYearService
) {

    suspend fun createFeesStructure(request: CreateFeesStructureRequest): FeesStructureDto {
        validateFeesStructureRequest(request.classId, request.academicYearId, request.name, request.amount)

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate
        val isDuplicate = feesStructureRepository.checkDuplicate(
            request.classId,
            request.academicYearId,
            request.name
        )
        if (isDuplicate) {
            throw ApiException(
                "A fee structure with this name already exists for this class and academic year",
                HttpStatusCode.Conflict
            )
        }

        val feesStructureId = feesStructureRepository.create(request)
        return getFeesStructureById(feesStructureId)
    }

    suspend fun bulkCreateFeesStructures(request: BulkCreateFeesStructureRequest): List<FeesStructureDto> {
        validateUUID(request.classId, "Class ID")
        validateUUID(request.academicYearId, "Academic Year ID")

        if (request.feeStructures.isEmpty()) {
            throw ApiException("Fee structures list cannot be empty", HttpStatusCode.BadRequest)
        }

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        val createRequests = mutableListOf<CreateFeesStructureRequest>()

        for (feeStructure in request.feeStructures) {
            validateFeesStructureName(feeStructure.name)
            validateAmount(feeStructure.amount)

            // Check for duplicate
            val isDuplicate = feesStructureRepository.checkDuplicate(
                request.classId,
                request.academicYearId,
                feeStructure.name
            )
            if (!isDuplicate) {
                createRequests.add(CreateFeesStructureRequest(
                    classId = request.classId,
                    academicYearId = request.academicYearId,
                    name = feeStructure.name,
                    amount = feeStructure.amount,
                    isMandatory = feeStructure.isMandatory
                ))
            }
        }

        if (createRequests.isEmpty()) {
            throw ApiException("All fee structures already exist for this class", HttpStatusCode.Conflict)
        }

        val feesStructureIds = feesStructureRepository.bulkCreate(createRequests)
        return feesStructureIds.map { getFeesStructureById(it) }
    }

    suspend fun getFeesStructureById(id: String): FeesStructureDto {
        validateUUID(id, "Fees Structure ID")
        return feesStructureRepository.findById(id)
            ?: throw ApiException("Fees Structure not found", HttpStatusCode.NotFound)
    }

    suspend fun getAllFeesStructures(): List<FeesStructureDto> {
        return feesStructureRepository.findAll()
    }

    suspend fun updateFeesStructure(id: String, request: UpdateFeesStructureRequest): FeesStructureDto {
        validateUUID(id, "Fees Structure ID")
        validateFeesStructureRequest(request.classId, request.academicYearId, request.name, request.amount)

        // Validate that referenced entities exist
        classService.getClassById(request.classId)
        academicYearService.getAcademicYearById(request.academicYearId)

        // Check for duplicate (excluding current record)
        val isDuplicate = feesStructureRepository.checkDuplicate(
            request.classId,
            request.academicYearId,
            request.name,
            excludeId = id
        )
        if (isDuplicate) {
            throw ApiException(
                "A fee structure with this name already exists for this class and academic year",
                HttpStatusCode.Conflict
            )
        }

        val updated = feesStructureRepository.update(id, request)
        if (!updated) {
            throw ApiException("Fees Structure not found", HttpStatusCode.NotFound)
        }

        return getFeesStructureById(id)
    }

    suspend fun deleteFeesStructure(id: String) {
        validateUUID(id, "Fees Structure ID")
        val deleted = feesStructureRepository.delete(id)
        if (!deleted) {
            throw ApiException("Fees Structure not found", HttpStatusCode.NotFound)
        }
    }

    suspend fun getFeesByClass(classId: String): List<FeesStructureDto> {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return feesStructureRepository.findByClassId(classId)
    }

    suspend fun getFeesByAcademicYear(academicYearId: String): List<FeesStructureDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return feesStructureRepository.findByAcademicYear(academicYearId)
    }

    suspend fun getFeesByClassAndAcademicYear(classId: String, academicYearId: String): List<FeesStructureDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return feesStructureRepository.findByClassAndAcademicYear(classId, academicYearId)
    }

    suspend fun getMandatoryFees(classId: String, academicYearId: String): List<FeesStructureDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return feesStructureRepository.findMandatoryFees(classId, academicYearId)
    }

    suspend fun getOptionalFees(classId: String, academicYearId: String): List<FeesStructureDto> {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")

        // Validate entities exist
        classService.getClassById(classId)
        academicYearService.getAcademicYearById(academicYearId)

        return feesStructureRepository.findOptionalFees(classId, academicYearId)
    }

    suspend fun getClassFeesStructures(academicYearId: String): List<ClassFeesStructureDto> {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return feesStructureRepository.getClassFeesStructures(academicYearId)
    }

    suspend fun getFeesStructureSummary(academicYearId: String): FeesStructureSummaryDto {
        validateUUID(academicYearId, "Academic Year ID")
        // Validate academic year exists
        academicYearService.getAcademicYearById(academicYearId)
        return feesStructureRepository.getFeesStructureSummary(academicYearId)
    }

    suspend fun removeAllFeesFromClass(classId: String): Int {
        validateUUID(classId, "Class ID")
        // Validate class exists
        classService.getClassById(classId)
        return feesStructureRepository.deleteByClassId(classId)
    }

    private fun validateFeesStructureRequest(classId: String, academicYearId: String, name: String, amount: String) {
        validateUUID(classId, "Class ID")
        validateUUID(academicYearId, "Academic Year ID")
        validateFeesStructureName(name)
        validateAmount(amount)
    }

    private fun validateFeesStructureName(name: String) {
        if (name.isBlank()) {
            throw ApiException("Fee structure name cannot be blank", HttpStatusCode.BadRequest)
        }
        if (name.length > 100) {
            throw ApiException("Fee structure name cannot exceed 100 characters", HttpStatusCode.BadRequest)
        }
    }

    private fun validateAmount(amount: String) {
        try {
            val amountValue = BigDecimal(amount)
            if (amountValue < BigDecimal.ZERO) {
                throw ApiException("Amount cannot be negative", HttpStatusCode.BadRequest)
            }
            if (amountValue.scale() > 2) {
                throw ApiException("Amount cannot have more than 2 decimal places", HttpStatusCode.BadRequest)
            }
        } catch (e: NumberFormatException) {
            throw ApiException("Invalid amount format", HttpStatusCode.BadRequest)
        }
    }

    private fun validateUUID(uuid: String, fieldName: String) {
        try {
            java.util.UUID.fromString(uuid)
        } catch (e: IllegalArgumentException) {
            throw ApiException("$fieldName must be a valid UUID", HttpStatusCode.BadRequest)
        }
    }
}