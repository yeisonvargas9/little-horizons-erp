package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ProfessorDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.Loan;
import com.littlehorizons.erp.model.LoanStatus;
import com.littlehorizons.erp.model.Professor;
import com.littlehorizons.erp.model.ProfessorStatus;
import com.littlehorizons.erp.model.Resource;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceStatus;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Business logic of the resources module: inventory, loans, returns and overdue loans. */
public class ResourceController {

    private static final int MAX_OPEN_LOANS_PER_BORROWER = 3;
    private static final int MAX_LOAN_DAYS = 30;

    private final ResourceDao resourceDao;
    private final LoanDao loanDao;
    private final StudentDao studentDao;
    private final ProfessorDao professorDao;

    public ResourceController(ResourceDao resourceDao, LoanDao loanDao, StudentDao studentDao, ProfessorDao professorDao) {
        this.resourceDao = resourceDao;
        this.loanDao = loanDao;
        this.studentDao = studentDao;
        this.professorDao = professorDao;
    }

    // ---------------------------------------------------------------- inventory

    public List<ResourceCategory> listCategories() {
        return resourceDao.findCategories();
    }

    /** Resources filtered (with Streams) by status and/or category; null means "no filter". */
    public List<Resource> listResources(ResourceStatus status, Integer categoryId) {
        return resourceDao.findAll().stream()
                .filter(r -> status == null || r.status() == status)
                .filter(r -> categoryId == null || r.categoryId() == categoryId)
                .collect(Collectors.toList());
    }

    public Optional<Resource> findResource(String code) {
        return resourceDao.findByCode(code);
    }

    public Resource registerResource(String code, String name, ResourceCategory category, String description,
                                     LocalDate acquisitionDate, String location) {
        require(code != null && code.matches("[A-Z]{2,4}-\\d{3,5}"), "El código debe ser como LIB-015 (letras, guion y números).");
        require(name != null && !name.isBlank(), "El nombre del recurso es obligatorio.");
        require(acquisitionDate == null || !acquisitionDate.isAfter(LocalDate.now()), "La fecha de adquisición no puede ser futura.");
        require(resourceDao.findByCode(code).isEmpty(), "Ya existe un recurso con el código " + code + ".");
        return resourceDao.save(new Resource(0, code, name, category.id(), category.name(), description,
                acquisitionDate, location, ResourceStatus.AVAILABLE));
    }

    /** A loaned resource cannot change status until it is returned, and a decommissioned one is final. */
    public void changeStatus(Resource resource, ResourceStatus newStatus) {
        require(resource.status() != ResourceStatus.LOANED, "El recurso está prestado: primero debe registrarse su devolución.");
        require(resource.status() != ResourceStatus.DECOMMISSIONED, "Un recurso dado de baja no puede volver a usarse.");
        require(newStatus != ResourceStatus.LOANED, "El estado 'Prestado' solo se asigna al registrar un préstamo.");
        require(resource.status() != newStatus, "El recurso ya está en estado " + newStatus.getLabel() + ".");
        require(resourceDao.updateStatus(resource.id(), newStatus), "No se pudo cambiar el estado.");
    }

    // ---------------------------------------------------------------- loans

    /**
     * Rules: the resource must be available; the borrower must exist and be active, with fewer than
     * {@value #MAX_OPEN_LOANS_PER_BORROWER} open loans; the return date is in the future, at most
     * {@value #MAX_LOAN_DAYS} days away.
     */
    public Loan lend(Resource resource, BorrowerType type, String borrowerDocument, LocalDate dueDate) {
        require(resource.status() == ResourceStatus.AVAILABLE,
                "El recurso no está disponible (estado: " + resource.status().getLabel() + ").");
        LocalDate today = LocalDate.now();
        require(dueDate != null && dueDate.isAfter(today), "La fecha de devolución debe ser posterior a hoy.");
        require(!dueDate.isAfter(today.plusDays(MAX_LOAN_DAYS)), "El préstamo no puede superar " + MAX_LOAN_DAYS + " días.");

        int borrowerId;
        String borrowerName;
        if (type == BorrowerType.STUDENT) {
            Student student = studentDao.findByDocument(borrowerDocument)
                    .orElseThrow(() -> new BusinessException("No existe un estudiante con ese documento."));
            require(student.getStatus() == StudentStatus.ACTIVE, "El estudiante no está activo.");
            borrowerId = student.getId();
            borrowerName = student.getFullName();
        } else {
            Professor professor = professorDao.findByDocument(borrowerDocument)
                    .orElseThrow(() -> new BusinessException("No existe un profesor con ese documento."));
            require(professor.getStatus() == ProfessorStatus.ACTIVE, "El profesor no está activo.");
            borrowerId = professor.getId();
            borrowerName = professor.getFullName();
        }
        loanDao.markOverdue(today);
        require(loanDao.countOpenByBorrower(type, borrowerId) < MAX_OPEN_LOANS_PER_BORROWER,
                borrowerName + " ya tiene " + MAX_OPEN_LOANS_PER_BORROWER + " préstamos sin devolver.");
        return loanDao.register(new Loan(0, resource.id(), resource.code(), resource.name(), type, borrowerId,
                borrowerName, today, dueDate, null, LoanStatus.ACTIVE, null));
    }

    /** Registers the return of a resource; returns the loan so the view can tell if it was late. */
    public Loan returnResource(Resource resource, String notes) {
        Loan loan = loanDao.findOpenByResource(resource.id())
                .orElseThrow(() -> new BusinessException("El recurso no tiene un préstamo pendiente de devolución."));
        LocalDate today = LocalDate.now();
        String finalNotes = today.isAfter(loan.dueDate())
                ? ("Devuelto con retraso" + (notes == null ? "" : ". " + notes)) : notes;
        loanDao.registerReturn(loan, today, finalNotes);
        return loan;
    }

    public List<Loan> listOpenLoans() {
        loanDao.markOverdue(LocalDate.now());
        return loanDao.findOpen();
    }

    /** Loans whose expected return date already passed, oldest first (Streams). */
    public List<Loan> listOverdueLoans() {
        return listOpenLoans().stream()
                .filter(loan -> loan.status() == LoanStatus.OVERDUE)
                .sorted((a, b) -> a.dueDate().compareTo(b.dueDate()))
                .collect(Collectors.toList());
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
