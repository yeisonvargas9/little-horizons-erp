package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.AcademicReportController;
import com.littlehorizons.erp.controller.ExtracurricularController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.ReportController;
import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.AcademicPerformanceDao;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.dao.ConceptDao;
import com.littlehorizons.erp.dao.ConfigurationDao;
import com.littlehorizons.erp.dao.CustomValueDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.MembershipDao;
import com.littlehorizons.erp.dao.ParticipationDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.ReportDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.impl.AcademicPerformanceDaoImpl;
import com.littlehorizons.erp.dao.impl.AcademicRecordDaoImpl;
import com.littlehorizons.erp.dao.impl.AssignmentDaoImpl;
import com.littlehorizons.erp.dao.impl.AttendanceDaoImpl;
import com.littlehorizons.erp.dao.impl.ChargeDaoImpl;
import com.littlehorizons.erp.dao.impl.ClubDaoImpl;
import com.littlehorizons.erp.dao.impl.ConceptDaoImpl;
import com.littlehorizons.erp.dao.impl.ConfigurationDaoImpl;
import com.littlehorizons.erp.dao.impl.CustomValueDaoImpl;
import com.littlehorizons.erp.dao.impl.EnrollmentDaoImpl;
import com.littlehorizons.erp.dao.impl.EventDaoImpl;
import com.littlehorizons.erp.dao.impl.GuardianDaoImpl;
import com.littlehorizons.erp.dao.impl.InvoiceDaoImpl;
import com.littlehorizons.erp.dao.impl.LoanDaoImpl;
import com.littlehorizons.erp.dao.impl.MembershipDaoImpl;
import com.littlehorizons.erp.dao.impl.ParticipationDaoImpl;
import com.littlehorizons.erp.dao.impl.PaymentDaoImpl;
import com.littlehorizons.erp.dao.impl.ProfessorDaoImpl;
import com.littlehorizons.erp.dao.impl.ReportDaoImpl;
import com.littlehorizons.erp.dao.impl.ResourceDaoImpl;
import com.littlehorizons.erp.dao.impl.StudentDaoImpl;
import com.littlehorizons.erp.dao.impl.SubjectDaoImpl;
import com.littlehorizons.erp.dao.impl.SubjectEnrollmentDaoImpl;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.InvoiceRenderer;
import com.littlehorizons.erp.util.PdfInvoiceRenderer;
import com.littlehorizons.erp.util.SqlExecutor;
import com.littlehorizons.erp.view.AcademicReportView;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.ExtracurricularView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ProfessorView;
import com.littlehorizons.erp.view.ReportView;
import com.littlehorizons.erp.view.ResourceView;
import com.littlehorizons.erp.view.StudentView;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

/** Application entry point: wires the layers together (manual dependency injection) and starts the UI. */
public class Main {

    public static void main(String[] args) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        AppLogger.info("Application started");
        try {
            DatabaseConnection database = DatabaseConnection.getInstance();
            database.verifyConnection();

            ConsoleInput input = new ConsoleInput(System.in);

            StudentDao studentDao = new StudentDaoImpl(database);
            GuardianDao guardianDao = new GuardianDaoImpl(database);
            StudentController studentController = new StudentController(studentDao, guardianDao);
            StudentView studentView = new StudentView(studentController, input);

            ProfessorDao professorDao = new ProfessorDaoImpl(database);
            SubjectDao subjectDao = new SubjectDaoImpl(database);
            AssignmentDao assignmentDao = new AssignmentDaoImpl(database);
            ProfessorController professorController = new ProfessorController(professorDao, subjectDao, assignmentDao);
            ProfessorView professorView = new ProfessorView(professorController, input);

            EnrollmentDao enrollmentDao = new EnrollmentDaoImpl(database);
            SubjectEnrollmentDao subjectEnrollmentDao = new SubjectEnrollmentDaoImpl(database);
            AcademicRecordDao recordDao = new AcademicRecordDaoImpl(database);
            AttendanceDao attendanceDao = new AttendanceDaoImpl(database);
            AcademicController academicController = new AcademicController(studentDao, subjectDao, enrollmentDao,
                    subjectEnrollmentDao, recordDao, attendanceDao, assignmentDao);
            SqlExecutor sqlExecutor = new SqlExecutor(database);
            AcademicPerformanceDao performanceDao = new AcademicPerformanceDaoImpl(sqlExecutor);
            AcademicReportController academicReportController = new AcademicReportController(performanceDao);
            AcademicReportView academicReportView = new AcademicReportView(academicReportController, input);
            AcademicView academicView = new AcademicView(academicController, input, academicReportView);

            ConceptDao conceptDao = new ConceptDaoImpl(database);
            CustomValueDao customValueDao = new CustomValueDaoImpl(database);
            ChargeDao chargeDao = new ChargeDaoImpl(database);
            PaymentDao paymentDao = new PaymentDaoImpl(database);
            InvoiceDao invoiceDao = new InvoiceDaoImpl(database);
            ConfigurationDao configurationDao = new ConfigurationDaoImpl(database);
            InvoiceRenderer invoiceRenderer = new PdfInvoiceRenderer(Path.of("invoices"));
            FinanceController financeController = new FinanceController(conceptDao, customValueDao, chargeDao, paymentDao,
                    invoiceDao, configurationDao, studentDao, guardianDao, invoiceRenderer);
            FinanceView financeView = new FinanceView(financeController, input);

            ClubDao clubDao = new ClubDaoImpl(sqlExecutor);
            MembershipDao membershipDao = new MembershipDaoImpl(sqlExecutor);
            EventDao eventDao = new EventDaoImpl(sqlExecutor);
            ParticipationDao participationDao = new ParticipationDaoImpl(sqlExecutor);
            ExtracurricularController extracurricularController = new ExtracurricularController(clubDao, membershipDao,
                    eventDao, participationDao, studentDao, professorDao);
            ExtracurricularView extracurricularView = new ExtracurricularView(extracurricularController, input);

            ResourceDao resourceDao = new ResourceDaoImpl(sqlExecutor);
            LoanDao loanDao = new LoanDaoImpl(sqlExecutor);
            ResourceController resourceController = new ResourceController(resourceDao, loanDao, studentDao, professorDao);
            ResourceView resourceView = new ResourceView(resourceController, input);

            ReportDao reportDao = new ReportDaoImpl(sqlExecutor);
            ReportController reportController = new ReportController(reportDao);
            ReportView reportView = new ReportView(reportController, input);

            new MainMenuView(input, studentView, academicView, professorView, financeView, extracurricularView,
                    resourceView, reportView).run();
        } catch (DataAccessException e) {
            System.out.println("✖ " + e.getMessage());
        } catch (IllegalStateException e) {
            AppLogger.warn("Console input closed: " + e.getMessage());
        } catch (RuntimeException e) {
            AppLogger.error("Unexpected error", e);
            System.out.println("✖ Ocurrió un error inesperado. Revise error_log.txt.");
        }
        AppLogger.info("Application finished");
    }
}
