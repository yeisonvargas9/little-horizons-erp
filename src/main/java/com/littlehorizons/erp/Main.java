package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.AcademicController;
import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.ConceptDao;
import com.littlehorizons.erp.dao.ConfigurationDao;
import com.littlehorizons.erp.dao.CustomValueDao;
import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.SubjectEnrollmentDao;
import com.littlehorizons.erp.dao.impl.AcademicRecordDaoImpl;
import com.littlehorizons.erp.dao.impl.AssignmentDaoImpl;
import com.littlehorizons.erp.dao.impl.AttendanceDaoImpl;
import com.littlehorizons.erp.dao.impl.ChargeDaoImpl;
import com.littlehorizons.erp.dao.impl.ConceptDaoImpl;
import com.littlehorizons.erp.dao.impl.ConfigurationDaoImpl;
import com.littlehorizons.erp.dao.impl.CustomValueDaoImpl;
import com.littlehorizons.erp.dao.impl.EnrollmentDaoImpl;
import com.littlehorizons.erp.dao.impl.GuardianDaoImpl;
import com.littlehorizons.erp.dao.impl.InvoiceDaoImpl;
import com.littlehorizons.erp.dao.impl.PaymentDaoImpl;
import com.littlehorizons.erp.dao.impl.ProfessorDaoImpl;
import com.littlehorizons.erp.dao.impl.StudentDaoImpl;
import com.littlehorizons.erp.dao.impl.SubjectDaoImpl;
import com.littlehorizons.erp.dao.impl.SubjectEnrollmentDaoImpl;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.InvoiceRenderer;
import com.littlehorizons.erp.util.PdfInvoiceRenderer;
import com.littlehorizons.erp.view.AcademicView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ProfessorView;
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
            AcademicView academicView = new AcademicView(academicController, input);

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

            new MainMenuView(input, studentView, academicView, professorView, financeView).run();
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
