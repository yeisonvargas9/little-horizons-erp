package com.littlehorizons.erp.view;

import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.util.ConsoleInput;

/** Main menu: groups the options by module and opens the corresponding sub-menu. */
public class MainMenuView {

    private final ConsoleInput input;
    private final StudentView studentView;
    private final AcademicView academicView;
    private final ProfessorView professorView;
    private final FinanceView financeView;

    public MainMenuView(ConsoleInput input, StudentView studentView, AcademicView academicView,
                        ProfessorView professorView, FinanceView financeView) {
        this.input = input;
        this.studentView = studentView;
        this.academicView = academicView;
        this.professorView = professorView;
        this.financeView = financeView;
    }

    public void run() {
        boolean exit = false;
        while (!exit) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 1, 8)) {
                    case 1 -> studentView.run();
                    case 2 -> academicView.run();
                    case 3 -> professorView.run();
                    case 5 -> financeView.run();
                    case 8 -> exit = true;
                    default -> System.out.println("\n  ⚠ Este módulo estará disponible próximamente.");
                }
            } catch (OperationCancelledException e) {
                System.out.println("  Operación cancelada.");
            }
        }
        System.out.println("\n¡Hasta pronto!");
    }

    private void printMenu() {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║   ERP Colegio Little Horizons                ║");
        System.out.println("║   Sistema de Gestión Educativa               ║");
        System.out.println("╚══════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("1. Gestión de Estudiantes");
        System.out.println("2. Gestión Académica");
        System.out.println("3. Gestión de Profesores");
        System.out.println("4. Actividades Extracurriculares");
        System.out.println("5. Gestión Financiera");
        System.out.println("6. Gestión de Recursos");
        System.out.println("7. Reportes y Estadísticas");
        System.out.println("8. Salir");
        System.out.println();
    }
}
