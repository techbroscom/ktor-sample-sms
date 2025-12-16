package com.example.plugins

import com.example.config.DropboxConfig
import com.example.config.SupabaseConfig
import com.example.repositories.AcademicYearRepository
import com.example.repositories.AttendanceRepository
import com.example.repositories.ClassRepository
import com.example.repositories.ClassSubjectRepository
import com.example.repositories.ComplaintRepository
import com.example.repositories.DashboardRepository
import com.example.repositories.ExamRepository
import com.example.repositories.ExamResultRepository
import com.example.repositories.ExamScheduleRepository
import com.example.repositories.FCMTokenRepository
import com.example.repositories.FeePaymentRepository
import com.example.repositories.FeesStructureRepository
import com.example.repositories.HolidayRepository
import com.example.repositories.OtpRepository
import com.example.repositories.PostRepository
import com.example.repositories.RulesAndRegulationsRepository
import com.example.repositories.SchoolConfigRepository
import com.example.repositories.StaffClassAssignmentRepository
import com.example.repositories.StaffSubjectAssignmentRepository
import com.example.repositories.StudentAssignmentRepository
import com.example.repositories.StudentFeeRepository
import com.example.repositories.StudentTransportAssignmentRepository
import com.example.repositories.SubjectRepository
import com.example.repositories.TransportRouteRepository
import com.example.repositories.TransportStopRepository
import com.example.repositories.UserRepository
import com.example.routes.api.academicYearRoutes
import com.example.routes.api.attendanceRoutes
import com.example.routes.api.classRoutes
import com.example.routes.api.classSubjectRoutes
import com.example.routes.api.complaintRoutes
import com.example.routes.api.dashboardRoutes
import com.example.routes.api.examResultRoutes
import com.example.routes.api.examRoutes
import com.example.routes.api.examScheduleRoutes
import com.example.routes.api.fcmRoutes
import com.example.routes.api.feePaymentRoutes
import com.example.routes.api.feesStructureRoutes
import com.example.routes.api.fileRoutes
import com.example.routes.api.holidayRoutes
import com.example.routes.api.postRoutes
import com.example.routes.api.rulesAndRegulationsRoutes
import com.example.routes.api.schoolConfigRoutes
import com.example.routes.api.staffClassAssignmentRoutes
import com.example.routes.api.staffClassSubjectRoutes
import com.example.routes.api.staffSubjectAssignmentRoutes
import com.example.routes.api.studentAssignmentRoutes
import com.example.routes.api.studentFeeRoutes
import com.example.routes.api.studentTransportAssignmentRoutes
import com.example.routes.api.subjectRoutes
import com.example.routes.api.tenantRoutes
import com.example.routes.api.transportRouteRoutes
import com.example.routes.api.transportStopRoutes
import com.example.routes.api.userRoutes
import com.example.services.AcademicYearService
import com.example.services.AttendanceService
import com.example.services.ClassService
import com.example.services.ClassSubjectService
import com.example.services.ComplaintService
import com.example.services.DashboardService
import com.example.services.EmailService
import com.example.services.ExamResultService
import com.example.services.ExamScheduleService
import com.example.services.ExamService
import com.example.services.FCMService
import com.example.services.FeePaymentService
import com.example.services.FeesStructureService
import com.example.services.FileService
import com.example.services.HolidayService
import com.example.services.LocalFileService
import com.example.services.NotificationService
import com.example.services.OtpService
import com.example.services.PostService
import com.example.services.RulesAndRegulationsService
import com.example.services.SchoolConfigService
import com.example.services.StaffClassAssignmentService
import com.example.services.StaffSubjectAssignmentService
import com.example.services.StudentAssignmentService
import com.example.services.StudentFeeService
import com.example.services.StudentTransportAssignmentService
import com.example.services.SubjectService
import com.example.services.TenantService
import com.example.services.TransportRouteService
import com.example.services.TransportStopService
import com.example.services.UserService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // Initialize dependencies
    val fcmTokenRepository = FCMTokenRepository()
    val fcmService = FCMService(fcmTokenRepository)


    val userRepository = UserRepository()
    val userService = UserService(userRepository, fcmService)

    val tenantService = TenantService()

    val notificationService = NotificationService(fcmService, userRepository)

    val holidayRepository = HolidayRepository()
    val holidayService = HolidayService(holidayRepository)

    val postRepository = PostRepository()
    val postService = PostService(postRepository, notificationService)

    val complaintRepository = ComplaintRepository()
    val complaintService = ComplaintService(complaintRepository)

    val academicYearRepository = AcademicYearRepository()
    val academicYearService = AcademicYearService(academicYearRepository)

    val otpRepository = OtpRepository()
    val emailService = EmailService()
    val otpService = OtpService(otpRepository, userRepository, emailService)

    val schoolConfigRepository = SchoolConfigRepository()
    val schoolConfigService = SchoolConfigService(schoolConfigRepository)

    val rulesAndRegulationsRepository = RulesAndRegulationsRepository()
    val rulesAndRegulationsService = RulesAndRegulationsService(rulesAndRegulationsRepository)

    val classRepository = ClassRepository()
    val classService = ClassService(classRepository, academicYearService)

    val subjectRepository = SubjectRepository()
    val subjectService = SubjectService(subjectRepository)

    val classSubjectRepository = ClassSubjectRepository()
    val classSubjectService = ClassSubjectService(classSubjectRepository, classService, subjectService, academicYearService)

    val studentAssignmentRepository = StudentAssignmentRepository()
    val studentAssignmentService = StudentAssignmentService(studentAssignmentRepository, userService, classService, academicYearService)

    val staffClassAssignmentRepository = StaffClassAssignmentRepository()
    val staffClassAssignmentService = StaffClassAssignmentService(staffClassAssignmentRepository, userService, classService, academicYearService)

    val staffSubjectAssignmentRepository = StaffSubjectAssignmentRepository()
    val staffSubjectAssignmentService = StaffSubjectAssignmentService(staffSubjectAssignmentRepository, userService, classService, classSubjectService, academicYearService)

    val examRepository = ExamRepository()
    val examService = ExamService(examRepository, classService, subjectService, academicYearService)

    val examScheduleRepository = ExamScheduleRepository()
    val examScheduleService = ExamScheduleService(examScheduleRepository, examService, classService)

    val examResultRepository = ExamResultRepository()
    val examResultService = ExamResultService(examResultRepository, examRepository, examService, userService)

    val attendanceRepository = AttendanceRepository()
    val attendanceService = AttendanceService(attendanceRepository, userService, classService, studentAssignmentService)

    val dashboardRepository = DashboardRepository()
    val dashboardService = DashboardService(dashboardRepository)

    val feesStructureRepository = FeesStructureRepository()
    val feesStructureService = FeesStructureService(feesStructureRepository, classService, academicYearService)

    val feePaymentRepository = FeePaymentRepository()
    val studentFeeRepository = StudentFeeRepository()

    val feePaymentService = FeePaymentService(feePaymentRepository, studentFeeRepository)

    val studentFeeService = StudentFeeService(studentFeeRepository, feePaymentRepository ,userService, feesStructureService)

    val transportRouteRepository = TransportRouteRepository()
    val transportRouteService = TransportRouteService(transportRouteRepository)

    val transportStopRepository = TransportStopRepository()
    val transportStopService = TransportStopService(transportStopRepository, transportRouteRepository)

    val studentTransportAssignmentRepository = StudentTransportAssignmentRepository()
    val studentTransportAssignmentService = StudentTransportAssignmentService(
        studentTransportAssignmentRepository,
        userRepository,
        academicYearRepository,
        transportRouteRepository,
        transportStopRepository
    )

    // NEW: Initialize FileService with Dropbox
    val dropboxConfig = DropboxConfig.fromEnvironment()

    val supabaseConfig = SupabaseConfig.forSupabase(
        projectUrl = "https://tzieatliufhksqtxbyoz.supabase.co",
        accessKeyId = "00dbbb297978d7097ab1865e46f765aa",
        secretAccessKey = "8f329a537c49a89d11f5010f03bbf412577f3a2d61437576fc60040e28b1d392",
        bucket = "sms"
    )

//    val fileService = FileService(supabaseConfig)

    val fileService = LocalFileService("/var/www/uploads")


    // API routes

    routing {
        get("/") {
            call.respondText("School Management Server")
        }

        get("/health") {
            call.respondText("OK")
        }

        // API routes

        tenantRoutes(tenantService)

        userRoutes(userService, otpService)
        holidayRoutes(holidayService)
        postRoutes(postService)
        complaintRoutes(complaintService)
        academicYearRoutes(academicYearService)
        schoolConfigRoutes(schoolConfigService)
        rulesAndRegulationsRoutes(rulesAndRegulationsService)
        classRoutes(classService)
        subjectRoutes(subjectService)
        classSubjectRoutes(classSubjectService)
        studentAssignmentRoutes(studentAssignmentService)
        staffClassAssignmentRoutes(staffClassAssignmentService)
        staffSubjectAssignmentRoutes(staffSubjectAssignmentService)
        examRoutes(examService)
        examScheduleRoutes(examScheduleService)
        examResultRoutes(examResultService)
        attendanceRoutes(attendanceService)
        dashboardRoutes(dashboardService)
        staffClassSubjectRoutes(staffClassAssignmentService, staffSubjectAssignmentService)
        fileRoutes(fileService)
        feesStructureRoutes(feesStructureService)
        fcmRoutes(fcmService)
        studentFeeRoutes(studentFeeService)
        feePaymentRoutes(feePaymentService)
        transportRouteRoutes(transportRouteService)
        transportStopRoutes(transportStopService)
        studentTransportAssignmentRoutes(studentTransportAssignmentService)
    }
}