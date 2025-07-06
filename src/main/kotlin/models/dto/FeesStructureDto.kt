package com.example.models.dto

import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class FeesStructureDto(
    val id: String? = null,
    val classId: String,
    val academicYearId: String,
    val name: String,
    val amount: String, // Using String for BigDecimal serialization
    val isMandatory: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    // Additional fields for joined queries
    val className: String? = null,
    val sectionName: String? = null,
    val academicYearName: String? = null
)

@Serializable
data class CreateFeesStructureRequest(
    val classId: String,
    val academicYearId: String,
    val name: String,
    val amount: String, // Using String for BigDecimal input
    val isMandatory: Boolean = true
)

@Serializable
data class UpdateFeesStructureRequest(
    val classId: String,
    val academicYearId: String,
    val name: String,
    val amount: String, // Using String for BigDecimal input
    val isMandatory: Boolean = true
)

@Serializable
data class BulkCreateFeesStructureRequest(
    val classId: String,
    val academicYearId: String,
    val feeStructures: List<FeesStructureItem>
)

@Serializable
data class FeesStructureItem(
    val name: String,
    val amount: String,
    val isMandatory: Boolean = true
)

@Serializable
data class ClassFeesStructureDto(
    val classId: String,
    val className: String,
    val sectionName: String,
    val academicYearId: String,
    val academicYearName: String,
    val feeStructures: List<FeesStructureDto>,
    val totalMandatoryFees: String,
    val totalOptionalFees: String,
    val totalFees: String
)

@Serializable
data class FeesStructureSummaryDto(
    val academicYearId: String,
    val academicYearName: String,
    val totalClasses: Int,
    val totalFeeStructures: Int,
    val totalMandatoryFees: String,
    val totalOptionalFees: String,
    val classSummaries: List<ClassFeesStructureDto>
)