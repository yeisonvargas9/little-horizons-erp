package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.ResourceController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.Validator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Console screens for the resources module (inventory, loans, returns and overdue loans). */
public class ResourceView {

    private static final String CANCEL_HINT = "(escriba 'cancelar' en cualquier momento para volver)";

    private final ResourceController controller;
    private final ConsoleInput input;

    public ResourceView(ResourceController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 0, 7)) {
                    case 1 -> showResources();
                    case 2 -> registerResource();
                    case 3 -> changeStatus();
                    case 4 -> lendResource();
                    case 5 -> returnResource();
                    case 6 -> printLoans("Préstamos sin devolver", controller.listOpenLoans());
                    case 7 -> printLoans("Préstamos vencidos", controller.listOverdueLoans());
                    case 0 -> back = true;
                    default -> { }
                }
            } catch (OperationCancelledException e) {
                System.out.println("  Operación cancelada.");
            } catch (BusinessException e) {
                System.out.println("  ⚠ " + e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("  ✖ " + e.getMessage());
            }
        }
    }

    private void printMenu() {
        System.out.println("\n=== GESTIÓN DE RECURSOS ===");
        System.out.println("1. Ver inventario de recursos");
        System.out.println("2. Registrar un recurso");
        System.out.println("3. Cambiar estado de un recurso (mantenimiento / baja)");
        System.out.println("4. Registrar un préstamo");
        System.out.println("5. Registrar una devolución");
        System.out.println("6. Ver préstamos sin devolver");
        System.out.println("7. Reporte de préstamos vencidos");
        System.out.println("0. Volver al menú principal");
        System.out.println();
    }

    private void showResources() {
        ResourceStatus status = null;
        if (input.readYesNo("¿Filtrar por estado?")) {
            status = input.readChoice("Estado", ResourceStatus.values(), ResourceStatus::getLabel);
        }
        Integer categoryId = null;
        if (input.readYesNo("¿Filtrar por categoría?")) {
            categoryId = askCategory().id();
        }
        List<Resource> resources = controller.listResources(status, categoryId);
        if (resources.isEmpty()) {
            System.out.println("  No hay recursos con ese filtro.");
            return;
        }
        System.out.printf("%n%-9s %-38s %-22s %-22s %s%n", "Código", "Recurso", "Categoría", "Ubicación", "Estado");
        System.out.println("-".repeat(108));
        resources.forEach(r -> System.out.printf("%-9s %-38s %-22s %-22s %s%n", r.code(), r.name(), r.categoryName(),
                r.location() == null ? "-" : r.location(), r.status().getLabel()));
        System.out.println("Total: " + resources.size());
    }

    private void registerResource() {
        System.out.println("\n--- Registrar recurso --- " + CANCEL_HINT);
        String code = input.readValidated("Código (ej: LIB-015)", s -> s.matches("[A-Z]{2,4}-\\d{3,5}"),
                "Use el formato LIB-015 (letras mayúsculas, guion y números).");
        String name = input.readRequired("Nombre del recurso");
        ResourceCategory category = askCategory();
        String description = input.readOptional("Descripción (opcional)");
        LocalDate acquired = input.readOptionalDate("Fecha de adquisición (AAAA-MM-DD, opcional)");
        String location = input.readOptional("Ubicación física (Biblioteca, Laboratorio 1...)");
        Resource saved = controller.registerResource(code, name, category, description, acquired, location);
        System.out.println("  ✔ Recurso " + saved.code() + " registrado y disponible.");
    }

    private void changeStatus() {
        Resource resource = askResource();
        System.out.println("  Estado actual de " + resource.name() + ": " + resource.status().getLabel());
        ResourceStatus[] options = {ResourceStatus.AVAILABLE, ResourceStatus.MAINTENANCE, ResourceStatus.DECOMMISSIONED};
        ResourceStatus status = input.readChoice("Nuevo estado", options, ResourceStatus::getLabel);
        controller.changeStatus(resource, status);
        System.out.println("  ✔ Estado cambiado a " + status.getLabel() + ".");
    }

    private void lendResource() {
        Resource resource = askResource();
        BorrowerType type = input.readChoice("¿A quién se presta?", BorrowerType.values(), BorrowerType::getLabel);
        String document = input.readValidated("Documento del " + type.getLabel().toLowerCase(), Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        LocalDate due = input.readOptionalDate("Fecha de devolución (AAAA-MM-DD, Enter = en 7 días)");
        Loan loan = controller.lend(resource, type, document, due == null ? LocalDate.now().plusDays(7) : due);
        System.out.println("  ✔ " + resource.name() + " prestado a " + loan.borrowerName() + ". Devolver el " + loan.dueDate() + ".");
    }

    private void returnResource() {
        Resource resource = askResource();
        String notes = input.readOptional("Observaciones de la devolución (opcional)");
        Loan loan = controller.returnResource(resource, notes);
        boolean late = LocalDate.now().isAfter(loan.dueDate());
        System.out.println("  ✔ Devolución registrada. " + resource.name() + " quedó Disponible."
                + (late ? " (Se devolvió con retraso)" : ""));
    }

    private void printLoans(String title, List<Loan> loans) {
        System.out.println("\n" + title + ":");
        if (loans.isEmpty()) {
            System.out.println("  No hay préstamos en esta lista.");
            return;
        }
        System.out.printf("%-9s %-30s %-10s %-26s %-11s %-11s %s%n", "Código", "Recurso", "Tipo", "Prestatario", "Prestado",
                "Devolver", "Estado");
        System.out.println("-".repeat(118));
        LocalDate today = LocalDate.now();
        loans.forEach(l -> System.out.printf("%-9s %-30s %-10s %-26s %-11s %-11s %s%s%n", l.resourceCode(), l.resourceName(),
                l.borrowerType().getLabel(), l.borrowerName(), l.loanDate(), l.dueDate(), l.status().getLabel(),
                l.dueDate().isBefore(today) ? " (" + ChronoUnit.DAYS.between(l.dueDate(), today) + " días de retraso)" : ""));
        System.out.println("Total: " + loans.size());
    }

    private Resource askResource() {
        String code = input.readRequired("Código del recurso (ej: LIB-001) " + CANCEL_HINT).toUpperCase();
        return controller.findResource(code)
                .orElseThrow(() -> new BusinessException("No existe un recurso con el código " + code + "."));
    }

    private ResourceCategory askCategory() {
        List<ResourceCategory> categories = controller.listCategories();
        return input.readChoice("Categoría", categories.toArray(new ResourceCategory[0]), ResourceCategory::name);
    }
}
