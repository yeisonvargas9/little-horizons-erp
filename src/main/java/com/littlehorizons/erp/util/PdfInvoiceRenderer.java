package com.littlehorizons.erp.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.littlehorizons.erp.exception.DataAccessException;
import com.littlehorizons.erp.model.Guardian;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoiceLine;
import com.littlehorizons.erp.model.SchoolSettings;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * Draws the electronic invoice as a PDF (OpenPDF) with a QR code (ZXing). The file is saved as
 * "factura_000123.pdf" inside the output folder (created if it does not exist).
 */
public class PdfInvoiceRenderer implements InvoiceRenderer {

    private static final Color BRAND = new Color(31, 78, 121);
    private static final Color LIGHT = new Color(234, 240, 247);
    private static final Locale CO = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Path outputFolder;

    public PdfInvoiceRenderer(Path outputFolder) {
        this.outputFolder = outputFolder;
    }

    @Override
    public Path render(Invoice invoice, SchoolSettings school, Guardian customer) {
        Font title = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, Font.BOLD, BRAND);
        Font bold = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.BLACK);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 9, Font.NORMAL, Color.BLACK);
        Font small = FontFactory.getFont(FontFactory.HELVETICA, 7, Font.NORMAL, Color.DARK_GRAY);
        Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Font.BOLD, Color.WHITE);

        try {
            Files.createDirectories(outputFolder);
            Path file = outputFolder.resolve(String.format("factura_%06d.pdf", invoice.sequence()));
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, new FileOutputStream(file.toFile()));
            document.open();

            // ---- header: school data (left) and invoice number (right)
            PdfPTable header = new PdfPTable(new float[]{3f, 2f});
            header.setWidthPercentage(100);
            PdfPCell left = cell(null, Rectangle.NO_BORDER, Element.ALIGN_LEFT);
            left.addElement(new Paragraph(school.legalName(), title));
            left.addElement(new Paragraph("NIT: " + school.nit(), normal));
            left.addElement(new Paragraph(school.address(), normal));
            left.addElement(new Paragraph("Tel: " + school.phone() + "  |  " + school.email(), normal));
            header.addCell(left);
            PdfPCell right = cell(null, Rectangle.BOX, Element.ALIGN_CENTER);
            right.setBackgroundColor(LIGHT);
            right.addElement(new Paragraph("FACTURA ELECTRÓNICA DE VENTA", bold));
            right.addElement(new Paragraph("No. " + invoice.number(), title));
            right.addElement(new Paragraph("Fecha de emisión: " + invoice.issueDate().format(DATE), normal));
            header.addCell(right);
            document.add(header);

            Paragraph resolution = new Paragraph(school.dianResolution() + "  |  Rango autorizado: " + school.invoicePrefix()
                    + "-" + String.format("%06d", school.rangeFrom()) + " a " + school.invoicePrefix() + "-"
                    + String.format("%06d", school.rangeTo()), small);
            resolution.setSpacingBefore(6);
            resolution.setSpacingAfter(10);
            document.add(resolution);

            // ---- customer
            PdfPTable customerTable = new PdfPTable(new float[]{1.2f, 4f});
            customerTable.setWidthPercentage(100);
            customerTable.addCell(headerCell("DATOS DEL CLIENTE", white, 2));
            addRow(customerTable, "Nombre:", customer.getFullName(), bold, normal);
            addRow(customerTable, "Documento:", customer.getDocumentType().getDbValue() + " " + customer.getDocumentNumber(), bold, normal);
            addRow(customerTable, "Dirección:", customer.getAddress() == null ? "-" : customer.getAddress(), bold, normal);
            document.add(customerTable);

            // ---- detail
            PdfPTable detail = new PdfPTable(new float[]{5f, 1f, 2f, 2f});
            detail.setWidthPercentage(100);
            detail.setSpacingBefore(12);
            for (String column : new String[]{"Descripción", "Cant.", "Valor unitario", "Subtotal"}) {
                detail.addCell(headerCell(column, white, 1));
            }
            for (InvoiceLine line : invoice.lines()) {
                detail.addCell(cell(line.description(), normal, Rectangle.BOTTOM, Element.ALIGN_LEFT));
                detail.addCell(cell(String.valueOf(line.quantity()), normal, Rectangle.BOTTOM, Element.ALIGN_CENTER));
                detail.addCell(cell(money(line.unitPrice()), normal, Rectangle.BOTTOM, Element.ALIGN_RIGHT));
                detail.addCell(cell(money(line.subtotal()), normal, Rectangle.BOTTOM, Element.ALIGN_RIGHT));
            }
            document.add(detail);

            // ---- totals
            PdfPTable totals = new PdfPTable(new float[]{6f, 2f});
            totals.setWidthPercentage(60);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setSpacingBefore(8);
            addTotal(totals, "Subtotal", money(invoice.subtotal()), normal);
            addTotal(totals, "IVA (" + school.vatPercent() + "% sobre conceptos gravados)", money(invoice.vat()), normal);
            addTotal(totals, "TOTAL A PAGAR", money(invoice.total()), bold);
            document.add(totals);

            // ---- DIAN elements: QR + CUFE
            PdfPTable legal = new PdfPTable(new float[]{1.3f, 4f});
            legal.setWidthPercentage(100);
            legal.setSpacingBefore(18);
            PdfPCell qrCell = cell(null, Rectangle.NO_BORDER, Element.ALIGN_LEFT);
            Image qr = qrImage(invoice.qrContent());
            qr.scaleAbsolute(95, 95);
            qrCell.addElement(qr);
            legal.addCell(qrCell);
            PdfPCell cufeCell = cell(null, Rectangle.NO_BORDER, Element.ALIGN_LEFT);
            cufeCell.addElement(new Paragraph("CUFE (Código Único de Factura Electrónica):", bold));
            cufeCell.addElement(new Paragraph(invoice.cufe(), small));
            Paragraph note = new Paragraph("Representación gráfica simulada con fines académicos. El CUFE, el código QR "
                    + "y la resolución de habilitación son simulados y no tienen validez ante la DIAN.", small);
            note.setSpacingBefore(8);
            cufeCell.addElement(note);
            legal.addCell(cufeCell);
            document.add(legal);

            document.close();
            return file;
        } catch (Exception e) {
            AppLogger.error("Could not generate the PDF of invoice " + invoice.number(), e);
            throw new DataAccessException("No fue posible generar el PDF de la factura " + invoice.number() + ".", e);
        }
    }

    // ---------------------------------------------------------------- helpers

    private Image qrImage(String content) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 300, 300,
                Map.of(EncodeHintType.CHARACTER_SET, "UTF-8", EncodeHintType.MARGIN, 1));
        BufferedImage image = new BufferedImage(matrix.getWidth(), matrix.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < matrix.getWidth(); x++) {
            for (int y = 0; y < matrix.getHeight(); y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        return Image.getInstance(bytes.toByteArray());
    }

    private static String money(BigDecimal value) {
        return String.format(CO, "$ %,.2f", value);
    }

    private PdfPCell cell(String text, int border, int alignment) {
        PdfPCell cell = new PdfPCell();
        if (text != null) {
            cell.setPhrase(new Phrase(text));
        }
        cell.setBorder(border);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private PdfPCell cell(String text, Font font, int border, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(border);
        cell.setPadding(5);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }

    private PdfPCell headerCell(String text, Font font, int columns) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(BRAND);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        cell.setColspan(columns);
        return cell;
    }

    private void addRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        table.addCell(cell(label, labelFont, Rectangle.NO_BORDER, Element.ALIGN_LEFT));
        table.addCell(cell(value, valueFont, Rectangle.NO_BORDER, Element.ALIGN_LEFT));
    }

    private void addTotal(PdfPTable table, String label, String value, Font font) {
        table.addCell(cell(label, font, Rectangle.NO_BORDER, Element.ALIGN_RIGHT));
        table.addCell(cell(value, font, Rectangle.NO_BORDER, Element.ALIGN_RIGHT));
    }
}
