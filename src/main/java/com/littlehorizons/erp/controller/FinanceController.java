package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ChargeDao;
import com.littlehorizons.erp.dao.ConceptDao;
import com.littlehorizons.erp.dao.ConfigurationDao;
import com.littlehorizons.erp.dao.CustomValueDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.exception.BusinessException;
import com.littlehorizons.erp.model.BillingResult;
import com.littlehorizons.erp.model.Charge;
import com.littlehorizons.erp.model.ChargeStatus;
import com.littlehorizons.erp.model.DelinquentSummary;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentMethod;
import com.littlehorizons.erp.model.SchoolSettings;
import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentStatus;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.InvoiceRenderer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Business logic of the financial module: payment concepts, personalized prices, charges, payments,
 * delinquent students and electronic invoices (PDF with simulated CUFE and QR).
 */
public class FinanceController {

    private static final int OVERDUE_MONTHS = 1;

    private final ConceptDao conceptDao;
    private final CustomValueDao customValueDao;
    private final ChargeDao chargeDao;
    private final PaymentDao paymentDao;
    private final InvoiceDao invoiceDao;
    private final ConfigurationDao configurationDao;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final InvoiceRenderer invoiceRenderer;

    public FinanceController(ConceptDao conceptDao, CustomValueDao customValueDao, ChargeDao chargeDao,
                             PaymentDao paymentDao, InvoiceDao invoiceDao, ConfigurationDao configurationDao,
                             StudentDao studentDao, GuardianDao guardianDao, InvoiceRenderer invoiceRenderer) {
        this.conceptDao = conceptDao;
        this.customValueDao = customValueDao;
        this.chargeDao = chargeDao;
        this.paymentDao = paymentDao;
        this.invoiceDao = invoiceDao;
        this.configurationDao = configurationDao;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.invoiceRenderer = invoiceRenderer;
    }

    // ---------------------------------------------------------------- lookups

    public Optional<Student> findStudent(String documentNumber) {
        return studentDao.findByDocument(documentNumber);
    }

    public Optional<Guardian> findGuardian(String documentNumber) {
        return guardianDao.findByDocument(documentNumber);
    }

    public List<Guardian> getGuardians(Student student) {
        return guardianDao.findByStudentId(student.getId());
    }

    public Optional<PaymentConcept> findConcept(int conceptId) {
        return conceptDao.findById(conceptId);
    }

    public int currentAcademicYear() {
        return Year.now().getValue();
    }

    // ---------------------------------------------------------------- concepts and prices

    public List<PaymentConcept> listConcepts() {
        return conceptDao.findAll();
    }

    public PaymentConcept addConcept(String code, String name, String description, BigDecimal baseAmount,
                                     boolean recurring, boolean appliesVat) {
        require(code != null && code.matches("[A-Z0-9_]{2,20}"), "El código debe tener de 2 a 20 letras mayúsculas, números o _.");
        require(name != null && !name.isBlank(), "El nombre del concepto es obligatorio.");
        require(baseAmount != null && baseAmount.signum() > 0, "El valor base debe ser mayor a cero.");
        require(conceptDao.findByCode(code).isEmpty(), "Ya existe un concepto con el código " + code + ".");
        return conceptDao.save(new PaymentConcept(0, code, name, description, baseAmount, recurring, appliesVat, true));
    }

    public void updateBaseAmount(PaymentConcept concept, BigDecimal newAmount) {
        require(newAmount != null && newAmount.signum() > 0, "El valor base debe ser mayor a cero.");
        require(conceptDao.updateBaseAmount(concept.id(), newAmount), "No se pudo actualizar el valor del concepto.");
    }

    /** Sets a personalized price (sibling discount, scholarship) used for this student's future charges. */
    public void customizeValue(Student student, PaymentConcept concept, BigDecimal amount, String reason) {
        require(amount != null && amount.signum() > 0, "El valor personalizado debe ser mayor a cero.");
        require(reason != null && !reason.isBlank(), "Indique el motivo (ej: Descuento por hermanos).");
        require(student.getStatus() == StudentStatus.ACTIVE, "El estudiante no está activo.");
        customValueDao.saveOrUpdate(student.getId(), concept.id(), amount, reason, currentAcademicYear());
    }

    // ---------------------------------------------------------------- charges

    /** Creates a charge using the student's personalized price if there is one, otherwise the base price. */
    public Charge createCharge(Student student, PaymentConcept concept, String description, LocalDate dueDate) {
        require(student.getStatus() == StudentStatus.ACTIVE, "Solo se pueden generar cargos a estudiantes activos.");
        require(concept.active(), "El concepto no está activo.");
        require(description != null && !description.isBlank(), "La descripción es obligatoria.");
        LocalDate today = LocalDate.now();
        require(dueDate != null && !dueDate.isBefore(today), "La fecha de vencimiento no puede ser anterior a hoy.");
        require(!chargeDao.existsByDescription(student.getId(), concept.id(), description),
                "Ya existe ese cargo para el estudiante.");
        int year = currentAcademicYear();
        BigDecimal amount = customValueDao.findAmount(student.getId(), concept.id(), year).orElse(concept.baseAmount());
        return chargeDao.save(new Charge(0, student.getId(), student.getFullName(), student.getDocumentNumber(),
                concept.id(), concept.name(), description, amount, today, dueDate, ChargeStatus.PENDING, year, null));
    }

    /** Unpaid charges of a student (first refreshes the "overdue" status). */
    public List<Charge> getUnpaidCharges(Student student) {
        chargeDao.markOverdue(LocalDate.now());
        return chargeDao.findUnpaidByStudent(student.getId());
    }

    // ---------------------------------------------------------------- payments and invoice

    /**
     * Confirms the payment of one or more charges of a student: saves the payments, marks the charges as paid,
     * creates the invoice and generates its PDF. If only the PDF fails the payment is kept (see
     * {@link #regenerateInvoice(String)}).
     */
    public BillingResult registerPayment(Student student, Guardian payer, List<Integer> chargeIds, PaymentMethod method) {
        require(!chargeIds.isEmpty(), "Seleccione al menos un cargo para pagar.");
        require(chargeIds.stream().distinct().count() == chargeIds.size(), "Hay cargos repetidos en la selección.");
        require(guardianDao.findByStudentId(student.getId()).stream().anyMatch(g -> g.getId() == payer.getId()),
                "El acudiente no está asociado a este estudiante.");

        List<Charge> charges = new ArrayList<>();
        for (int id : chargeIds) {
            Charge charge = chargeDao.findById(id)
                    .orElseThrow(() -> new BusinessException("No existe el cargo " + id + "."));
            require(charge.studentId() == student.getId(), "El cargo " + id + " no pertenece a este estudiante.");
            require(charge.status() != ChargeStatus.PAID, "El cargo " + id + " ya está pagado.");
            charges.add(charge);
        }

        SchoolSettings school = SchoolSettings.fromMap(configurationDao.findAll());
        int sequence = invoiceDao.nextSequence();
        require(sequence >= school.rangeFrom() && sequence <= school.rangeTo(),
                "Se agotó el rango de numeración de facturas autorizado por la DIAN (simulado).");

        LocalDate today = LocalDate.now();
        List<Payment> payments = new ArrayList<>();
        List<InvoiceLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal vatDivisor = BigDecimal.valueOf(100 + school.vatPercent()).movePointLeft(2);
        for (Charge charge : charges) {
            boolean taxed = conceptDao.findById(charge.conceptId()).map(PaymentConcept::appliesVat).orElse(false);
            BigDecimal gross = charge.amount();
            // Prices already include VAT: the base is gross / (1 + rate) and the VAT is the difference.
            BigDecimal base = taxed ? gross.divide(vatDivisor, 2, RoundingMode.HALF_UP) : gross;
            subtotal = subtotal.add(base);
            vat = vat.add(gross.subtract(base));
            lines.add(new InvoiceLine(charge.description() + " - " + charge.studentName(), 1, base, base, null));
            payments.add(new Payment(0, student.getId(), student.getFullName(), payer.getId(), charge.conceptId(),
                    charge.conceptName(), charge.id(), gross, today, method, null, charge.academicYear(),
                    charge.period(), null));
        }
        BigDecimal total = subtotal.add(vat);
        String number = String.format("%s-%06d", school.invoicePrefix(), sequence);
        String cufe = computeCufe(number, today, school.nit(), total);
        Invoice invoice = new Invoice(0, sequence, number, payer.getId(), today, subtotal, vat, total, cufe,
                buildQrContent(number, today, school, payer, subtotal, vat, total, cufe), null, null, lines);

        BillingResult saved = paymentDao.registerPayments(payments, invoice);
        return generatePdf(saved, school, payer);
    }

    /** Creates the PDF again for an existing invoice (for example if the first attempt failed). */
    public Invoice regenerateInvoice(String invoiceNumber) {
        Invoice invoice = invoiceDao.findByNumber(invoiceNumber)
                .orElseThrow(() -> new BusinessException("No existe la factura " + invoiceNumber + "."));
        Guardian customer = guardianDao.findById(invoice.guardianId())
                .orElseThrow(() -> new BusinessException("No se encontró el acudiente de la factura."));
        SchoolSettings school = SchoolSettings.fromMap(configurationDao.findAll());
        BillingResult result = generatePdf(new BillingResult(invoice, List.of()), school, customer);
        require(result.invoice().pdfPath() != null, "No fue posible generar el PDF. Revise error_log.txt.");
        return result.invoice();
    }

    public List<Invoice> getInvoices(Guardian guardian) {
        return invoiceDao.findByGuardian(guardian.getId());
    }

    // ---------------------------------------------------------------- history and delinquency

    public List<Payment> getPaymentsByStudent(Student student) {
        return paymentDao.findByStudent(student.getId());
    }

    public List<Payment> getPaymentsByGuardian(Guardian guardian) {
        return paymentDao.findByGuardian(guardian.getId());
    }

    /** Students with unpaid charges whose due date passed more than one month ago (grouped with Streams). */
    public List<DelinquentSummary> findDelinquents() {
        LocalDate today = LocalDate.now();
        chargeDao.markOverdue(today);
        Map<Integer, List<Charge>> byStudent = chargeDao.findUnpaidDueBefore(today.minusMonths(OVERDUE_MONTHS)).stream()
                .collect(Collectors.groupingBy(Charge::studentId));
        return byStudent.values().stream()
                .map(charges -> new DelinquentSummary(charges.get(0).studentId(), charges.get(0).studentName(),
                        charges.get(0).studentDocument(), charges.size(),
                        charges.stream().map(Charge::amount).reduce(BigDecimal.ZERO, BigDecimal::add),
                        charges.stream().map(Charge::dueDate).min(Comparator.naturalOrder()).orElse(today)))
                .sorted(Comparator.comparing(DelinquentSummary::totalDue).reversed())
                .collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- private helpers

    private BillingResult generatePdf(BillingResult result, SchoolSettings school, Guardian customer) {
        try {
            Path file = invoiceRenderer.render(result.invoice(), school, customer);
            invoiceDao.updatePdfLocation(result.invoice().id(), file.getFileName().toString(), file.toAbsolutePath().toString());
            return new BillingResult(result.invoice().withPdf(file.getFileName().toString(),
                    file.toAbsolutePath().toString()), result.payments());
        } catch (RuntimeException e) {
            AppLogger.warn("Invoice " + result.invoice().number() + " saved but its PDF could not be generated");
            return result;
        }
    }

    /** Simulated CUFE: SHA-384 hash of the invoice number + date + school NIT + total. */
    private String computeCufe(String number, LocalDate date, String nit, BigDecimal total) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-384");
            byte[] hash = digest.digest((number + date + nit + total.toPlainString()).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 is not available", e);
        }
    }

    private String buildQrContent(String number, LocalDate date, SchoolSettings school, Guardian customer,
                                  BigDecimal subtotal, BigDecimal vat, BigDecimal total, String cufe) {
        return "NumFac: " + number + "\nFecFac: " + date + "\nNitFac: " + school.nit()
                + "\nDocAdq: " + customer.getDocumentNumber() + "\nValFac: " + subtotal.toPlainString()
                + "\nValIva: " + vat.toPlainString() + "\nValTotal: " + total.toPlainString()
                + "\nCUFE: " + cufe;
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BusinessException(message);
        }
    }
}
