package com.littlehorizons.erp;

import com.littlehorizons.erp.controller.ProfessorController;
import com.littlehorizons.erp.controller.StudentController;
import com.littlehorizons.erp.dao.AssignmentDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.SubjectDao;
import com.littlehorizons.erp.dao.impl.AssignmentDaoImpl;
import com.littlehorizons.erp.dao.impl.GuardianDaoImpl;
import com.littlehorizons.erp.dao.impl.ProfessorDaoImpl;
import com.littlehorizons.erp.dao.impl.StudentDaoImpl;
import com.littlehorizons.erp.dao.impl.SubjectDaoImpl;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.view.MainMenuView;
import com.littlehorizons.erp.view.ProfessorView;
import com.littlehorizons.erp.view.StudentView;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
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

            new MainMenuView(input, studentView, professorView).run();
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
