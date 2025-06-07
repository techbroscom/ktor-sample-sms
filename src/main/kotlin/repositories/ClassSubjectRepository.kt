package com.example.repositories

import com.example.database.tables.AcademicYears
import com.example.database.tables.ClassSubjects
import com.example.database.tables.Classes
import com.example.database.tables.Subjects
import com.example.models.dto.*
import com.example.utils.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.*

class ClassSubjectRepository {

    suspend fun create(request: CreateClassSubjectRequest): String = dbQuery {
        ClassSubjects.insert {
            it[classId] = UUID.fromString(request.classId)
            it[subjectId] = UUID.fromString(request.subjectId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
        }[ClassSubjects.id].toString()
    }

    suspend fun bulkCreate(requests: List<CreateClassSubjectRequest>): List<String> = dbQuery {
        requests.map { request ->
            ClassSubjects.insert {
                it[classId] = UUID.fromString(request.classId)
                it[subjectId] = UUID.fromString(request.subjectId)
                it[academicYearId] = UUID.fromString(request.academicYearId)
            }[ClassSubjects.id].toString()
        }
    }

    suspend fun findById(id: String): ClassSubjectDto? = dbQuery {
        ClassSubjects
            .join(Classes, JoinType.LEFT, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.id eq UUID.fromString(id) }
            .map { mapRowToDto(it) }
            .singleOrNull()
    }

    suspend fun findAll(): List<ClassSubjectDto> = dbQuery {
        ClassSubjects
            .join(Classes, JoinType.LEFT, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun update(id: String, request: UpdateClassSubjectRequest): Boolean = dbQuery {
        ClassSubjects.update({ ClassSubjects.id eq UUID.fromString(id) }) {
            it[classId] = UUID.fromString(request.classId)
            it[subjectId] = UUID.fromString(request.subjectId)
            it[academicYearId] = UUID.fromString(request.academicYearId)
        } > 0
    }

    suspend fun delete(id: String): Boolean = dbQuery {
        ClassSubjects.deleteWhere { ClassSubjects.id eq UUID.fromString(id) } > 0
    }

    suspend fun findByClassId(classId: String): List<ClassSubjectDto> = dbQuery {
        ClassSubjects
            .join(Classes, JoinType.LEFT, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.classId eq UUID.fromString(classId) }
            .orderBy(Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findBySubjectId(subjectId: String): List<ClassSubjectDto> = dbQuery {
        ClassSubjects
            .join(Classes, JoinType.LEFT, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.subjectId eq UUID.fromString(subjectId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findBySubjectAndAcademicYear(subjectId: String, academicYearId: String): List<ClassSubjectDto> = dbQuery {
        ClassSubjects
            .join(Classes, JoinType.LEFT, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where { (ClassSubjects.subjectId eq UUID.fromString(subjectId)) and (AcademicYears.id eq UUID.fromString(academicYearId))}
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByAcademicYear(academicYearId: String): List<ClassSubjectDto> = dbQuery {
        ClassSubjects
            .join(Classes, JoinType.LEFT, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun findByClassAndAcademicYear(classId: String, academicYearId: String): List<ClassSubjectDto> = dbQuery {
        ClassSubjects
            .join(Classes, JoinType.LEFT, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.LEFT, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.LEFT, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where {
                (ClassSubjects.classId eq UUID.fromString(classId)) and
                        (ClassSubjects.academicYearId eq UUID.fromString(academicYearId))
            }
            .orderBy(Subjects.name to SortOrder.ASC)
            .map { mapRowToDto(it) }
    }

    suspend fun checkDuplicate(classId: String, subjectId: String, academicYearId: String, excludeId: String? = null): Boolean = dbQuery {
        val query = ClassSubjects.selectAll()
            .where {
                (ClassSubjects.classId eq UUID.fromString(classId)) and
                        (ClassSubjects.subjectId eq UUID.fromString(subjectId)) and
                        (ClassSubjects.academicYearId eq UUID.fromString(academicYearId))
            }

        if (excludeId != null) {
            query.andWhere { ClassSubjects.id neq UUID.fromString(excludeId) }
        }

        query.count() > 0
    }

    suspend fun deleteByClassId(classId: String): Int = dbQuery {
        ClassSubjects.deleteWhere { ClassSubjects.classId eq UUID.fromString(classId) }
    }

    suspend fun deleteByClassAndAcademicYear(classId: String, academicYearId: String): Int = dbQuery {
        ClassSubjects.deleteWhere { (ClassSubjects.classId eq UUID.fromString(classId)) and (ClassSubjects.academicYearId eq UUID.fromString(academicYearId)) }
    }

    suspend fun deleteBySubjectId(subjectId: String): Int = dbQuery {
        ClassSubjects.deleteWhere { ClassSubjects.subjectId eq UUID.fromString(subjectId) }
    }

    suspend fun deleteBySubjectAndAcademicYear(subjectId: String, academicYearId: String): Int = dbQuery {
        ClassSubjects.deleteWhere { (ClassSubjects.subjectId eq UUID.fromString(subjectId)) and (ClassSubjects.academicYearId eq UUID.fromString(academicYearId)) }
    }

    suspend fun deleteByAcademicYear(academicYearId: String): Int = dbQuery {
        ClassSubjects.deleteWhere { ClassSubjects.academicYearId eq UUID.fromString(academicYearId) }
    }

    suspend fun getClassesWithSubjects(academicYearId: String): List<ClassWithSubjectsDto> = dbQuery {
        val classSubjects = ClassSubjects
            .join(Classes, JoinType.INNER, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.INNER, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Classes.className to SortOrder.ASC, Classes.sectionName to SortOrder.ASC)
            .toList()

        classSubjects.groupBy {
            Triple(
                it[ClassSubjects.classId].toString(),
                it[Classes.className],
                it[Classes.sectionName]
            )
        }.map { (classInfo, rows) ->
            ClassWithSubjectsDto(
                classId = classInfo.first,
                className = classInfo.second,
                sectionName = classInfo.third,
                academicYearId = academicYearId,
                academicYearName = rows.first()[AcademicYears.year] ?: "",
                subjects = rows.map { row ->
                    SubjectDto(
                        id = row[ClassSubjects.subjectId].toString(),
                        name = row[Subjects.name],
                        code = row[Subjects.code]
                    )
                }
            )
        }
    }

    suspend fun getSubjectsWithClasses(academicYearId: String): List<SubjectWithClassesDto> = dbQuery {
        val classSubjects = ClassSubjects
            .join(Classes, JoinType.INNER, ClassSubjects.classId, Classes.id)
            .join(Subjects, JoinType.INNER, ClassSubjects.subjectId, Subjects.id)
            .join(AcademicYears, JoinType.INNER, ClassSubjects.academicYearId, AcademicYears.id)
            .selectAll()
            .where { ClassSubjects.academicYearId eq UUID.fromString(academicYearId) }
            .orderBy(Subjects.name to SortOrder.ASC)
            .toList()

        classSubjects.groupBy {
            Triple(
                it[ClassSubjects.subjectId].toString(),
                it[Subjects.name],
                it[Subjects.code]
            )
        }.map { (subjectInfo, rows) ->
            SubjectWithClassesDto(
                subjectId = subjectInfo.first,
                subjectName = subjectInfo.second,
                subjectCode = subjectInfo.third,
                classes = rows.map { row ->
                    ClassDto(
                        id = row[ClassSubjects.classId].toString(),
                        className = row[Classes.className],
                        sectionName = row[Classes.sectionName],
                        academicYearId = row[ClassSubjects.academicYearId].toString(),
                        academicYearName = row[AcademicYears.year]
                    )
                }
            )
        }
    }

    private fun mapRowToDto(row: ResultRow): ClassSubjectDto {
        return ClassSubjectDto(
            id = row[ClassSubjects.id].toString(),
            classId = row[ClassSubjects.classId].toString(),
            subjectId = row[ClassSubjects.subjectId].toString(),
            academicYearId = row[ClassSubjects.academicYearId].toString(),
            className = row.getOrNull(Classes.className),
            sectionName = row.getOrNull(Classes.sectionName),
            subjectName = row.getOrNull(Subjects.name),
            subjectCode = row.getOrNull(Subjects.code),
            academicYearName = row.getOrNull(AcademicYears.year)
        )
    }
}