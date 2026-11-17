package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.FinanceController;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.exception.OperationCancelledException;
import com.littlehorizons.erp.model.BillingResult;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.DelinquentSummary;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.Validator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/** Console screens for the financial module (concepts, prices, charges, payments, invoices and delinquents). */
public class FinanceView {

    private static final String CANCEL_HINT = "(escriba 'cancelar' en cualquier momento para volver)";
    private static final Locale CO = Locale.forLanguageTag("es-CO");

    private final FinanceController controller;
    private final ConsoleInput input;

    public FinanceView(FinanceController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void run() {
        boolean back = false;
        while (!back) {
            printMenu();
            try {
                switch (input.readInt("Seleccione una opción", 0, 10)) {
                    case 1 -> showConcepts();
                    case 2 -> addConcept();
                    case 3 -> updateConceptAmount();
                    case 4 -> customizeValue();
                    case 5 -> createCharge();
                    case 6 -> registerPayment();
                    case 7 -> showStudentPayments();
                    case 8 -> showGuardianPayments();
                    case 9 -> showDelinquents();
                    case 10 -> regenerateInvoice();
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
        System.out.println("\n=== GESTIÓN FINANCIERA ===");
        System.out.println("1. Ver catálogo de conceptos de pago");
        System.out.println("2. Agregar concepto de pago");
        System.out.println("3. Cambiar el valor base de un concepto");
        System.out.println("4. Personalizar valor para un estudiante (descuento / beca)");
        System.out.println("5. Generar un cargo a un estudiante");
        System.out.println("6. Registrar pago y generar factura PDF");
        System.out.println("7. Historial de pagos de un estudiante");
        System.out.println("8. Historial de pagos y facturas de un acudiente");
        System.out.println("9. Reporte de morosos (pendientes por más de un mes)");
        System.out.println("10. Regenerar el PDF de una factura");
        System.out.println("0. Volver al menú principal");
        System.out.println();
    }

    // ---------------------------------------------------------------- concepts and prices

    private void showConcepts() {
        printConcepts(controller.listConcepts());
    }

    private void addConcept() {
        System.out.println("\n--- Nuevo concepto de pago --- " + CANCEL_HINT);
        String code = input.readValidated("Código (letras mayúsculas, ej: BIBLIOTECA)", s -> s.matches("[A-Z0-9_]{2,20}"),
                "Use de 2 a 20 letras mayúsculas, números o _.");
        String name = input.readRequired("Nombre del concepto");
        String description = input.readOptional("Descripción (opcional)");
        BigDecimal amount = readMoney("Valor base en pesos");
        boolean recurring = input.readYesNo("¿Es un cobro recurrente (mensual)?");
        boolean vat = input.readYesNo("¿Está gravado con IVA?");
        PaymentConcept saved = controller.addConcept(code, name, description, amount, recurring, vat);
        System.out.println("  ✔ Concepto creado con ID " + saved.id() + ".");
    }

    private void updateConceptAmount() {
        PaymentConcept concept = askConcept();
        System.out.println("  Valor actual de " + concept.name() + ": " + money(concept.baseAmount()));
        controller.updateBaseAmount(concept, readMoney("Nuevo valor base en pesos"));
        System.out.println("  ✔ Valor actualizado. Los cargos ya generados no cambian.");
    }

    private void customizeValue() {
        Student student = askStudent();
        PaymentConcept concept = askConcept();
        System.out.println("  Valor base de " + concept.name() + ": " + money(concept.baseAmount()));
        BigDecimal amount = readMoney("Valor personalizado para " + student.getFirstName());
        String reason = input.readRequired("Motivo (ej: Descuento por hermanos, Beca 30%)");
        controller.customizeValue(student, concept, amount, reason);
        System.out.println("  ✔ Valor personalizado guardado. Se usará en los próximos cargos de ese concepto.");
    }

    // ---------------------------------------------------------------- charges and payments

    private void createCharge() {
        Student student = askStudent();
        PaymentConcept concept = askConcept();
        String description = input.readRequired("Descripción del cargo (ej: Pensión diciembre 2026)");
        LocalDate dueDate = input.readOptionalDate("Fecha de vencimiento (AAAA-MM-DD, Enter = en 10 días)");
        Charge charge = controller.createCharge(student, concept, description,
                dueDate == null ? LocalDate.now().plusDays(10) : dueDate);
        System.out.println("  ✔ Cargo " + charge.id() + " creado por " + money(charge.amount()) + ", vence el " + charge.dueDate() + ".");
    }

    private void registerPayment() {
        System.out.println("\n--- Registrar pago --- " + CANCEL_HINT);
        Student student = askStudent();
        List<Charge> unpaid = controller.getUnpaidCharges(student);
        if (unpaid.isEmpty()) {
            System.out.println("  El estudiante no tiene cargos pendientes.");
            return;
        }
        printCharges(unpaid);
        String answer = input.readValidated("IDs de los cargos a pagar, separados por coma (ej: 12,13)",
                s -> s.matches("\\d+(\\s*,\\s*\\d+)*"), "Escriba números separados por coma.");
        List<Integer> chargeIds = Arrays.stream(answer.split(",")).map(String::trim).map(Integer::parseInt).collect(Collectors.toList());

        List<Guardian> guardians = controller.getGuardians(student);
        Guardian payer = guardians.size() == 1 ? guardians.get(0)
                : input.readChoice("¿Quién realiza el pago?", guardians.toArray(new Guardian[0]),
                g -> g.getFullName() + " (" + g.getRelationship() + ")");
        PaymentMethod method = input.readChoice("Método de pago", PaymentMethod.values(), PaymentMethod::getLabel);
        BigDecimal total = unpaid.stream().filter(c -> chargeIds.contains(c.id())).map(Charge::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (!input.readYesNo("¿Confirmar el pago de " + money(total) + " realizado por " + payer.getFullName() + "?")) {
            System.out.println("  Pago no confirmado.");
            return;
        }

        BillingResult result = controller.registerPayment(student, payer, chargeIds, method);
        System.out.println("\n  ✔ Pago registrado. Recibos: "
                + result.payments().stream().map(Payment::receiptNumber).collect(Collectors.joining(", ")));
        Invoice invoice = result.invoice();
        System.out.println("  ✔ Factura " + invoice.number() + " por " + money(invoice.total()) + " (IVA " + money(invoice.vat()) + ")");
        if (invoice.pdfPath() == null) {
            System.out.println("  ⚠ El pago quedó guardado, pero no se pudo crear el PDF. Use la opción 10 para generarlo de nuevo.");
        } else {
            System.out.println("  ✔ PDF guardado en: " + invoice.pdfPath());
        }
    }

    private void regenerateInvoice() {
        String number = input.readRequired("Número de factura (ej: LH-000152) " + CANCEL_HINT).toUpperCase();
        Invoice invoice = controller.regenerateInvoice(number);
        System.out.println("  ✔ PDF generado en: " + invoice.pdfPath());
    }

    // ---------------------------------------------------------------- history and reports

    private void showStudentPayments() {
        Student student = askStudent();
        printPayments("Pagos de " + student.getFullName(), controller.getPaymentsByStudent(student));
    }

    private void showGuardianPayments() {
        String document = input.readValidated("Documento del acudiente " + CANCEL_HINT, Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        Guardian guardian = controller.findGuardian(document)
                .orElseThrow(() -> new BusinessException("No existe un acudiente con el documento " + document + "."));
        printPayments("Pagos realizados por " + guardian.getFullName(), controller.getPaymentsByGuardian(guardian));
        List<Invoice> invoices = controller.getInvoices(guardian);
        System.out.println("\nFacturas emitidas: " + invoices.size());
        invoices.forEach(i -> System.out.printf("  %-12s %s  %16s  %s%n", i.number(), i.issueDate(), money(i.total()),
                i.pdfFileName() == null ? "(sin PDF)" : i.pdfFileName()));
    }

    private void showDelinquents() {
        List<DelinquentSummary> delinquents = controller.findDelinquents();
        System.out.println("\nEstudiantes con pagos pendientes por más de un mes:");
        if (delinquents.isEmpty()) {
            System.out.println("  Ningún estudiante tiene pagos atrasados.");
            return;
        }
        System.out.printf("%-12s %-34s %8s %16s %s%n", "Documento", "Estudiante", "Cargos", "Total adeudado", "Vencido desde");
        System.out.println("-".repeat(88));
        delinquents.forEach(d -> System.out.printf("%-12s %-34s %8d %16s %s%n", d.studentDocument(), d.studentName(),
                d.pendingCharges(), money(d.totalDue()), d.oldestDueDate()));
        System.out.println("Total de morosos: " + delinquents.size() + "  |  Cartera vencida: "
                + money(delinquents.stream().map(DelinquentSummary::totalDue).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    // ---------------------------------------------------------------- helpers

    private Student askStudent() {
        String document = input.readValidated("Documento del estudiante " + CANCEL_HINT, Validator::isValidDocument,
                "Use entre 5 y 20 letras o números.");
        return controller.findStudent(document)
                .orElseThrow(() -> new BusinessException("No existe un estudiante con el documento " + document + "."));
    }

    private PaymentConcept askConcept() {
        printConcepts(controller.listConcepts());
        int id = input.readInt("ID del concepto", 1, Integer.MAX_VALUE);
        return controller.findConcept(id).orElseThrow(() -> new BusinessException("No existe un concepto con ID " + id + "."));
    }

    private BigDecimal readMoney(String prompt) {
        return new BigDecimal(input.readValidated(prompt, s -> s.matches("\\d{1,9}(\\.\\d{1,2})?"),
                "Escriba un valor numérico sin puntos de miles (ej: 350000)."));
    }

    private void printConcepts(List<PaymentConcept> concepts) {
        System.out.printf("%n%-4s %-14s %-28s %16s %-9s %s%n", "ID", "Código", "Concepto", "Valor base", "Recurrente", "IVA");
        System.out.println("-".repeat(86));
        concepts.forEach(c -> System.out.printf("%-4d %-14s %-28s %16s %-9s %s%n", c.id(), c.code(), c.name(),
                money(c.baseAmount()), c.recurring() ? "Sí" : "No", c.appliesVat() ? "Sí" : "No"));
    }

    private void printCharges(List<Charge> charges) {
        System.out.printf("%n%-6s %-36s %-11s %16s %-9s%n", "ID", "Descripción", "Vence", "Valor", "Estado");
        System.out.println("-".repeat(84));
        charges.forEach(c -> System.out.printf("%-6d %-36s %-11s %16s %-9s%n", c.id(), c.description(), c.dueDate(),
                money(c.amount()), c.status().getDbValue()));
    }

    private void printPayments(String title, List<Payment> payments) {
        System.out.println("\n" + title);
        if (payments.isEmpty()) {
            System.out.println("  Sin pagos registrados.");
            return;
        }
        System.out.printf("%-11s %-12s %-26s %-16s %16s%n", "Fecha", "Recibo", "Concepto", "Método", "Monto");
        System.out.println("-".repeat(86));
        payments.forEach(p -> System.out.printf("%-11s %-12s %-26s %-16s %16s%n", p.paymentDate(), p.receiptNumber(),
                p.conceptName(), p.method().getLabel(), money(p.amount())));
        System.out.println("Pagos: " + payments.size() + "  |  Total pagado: "
                + money(payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add)));
    }

    private String money(BigDecimal value) {
        return String.format(CO, "$ %,.0f", value);
    }
}
