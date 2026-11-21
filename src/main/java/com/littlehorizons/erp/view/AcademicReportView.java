package com.littlehorizons.erp.view;

import com.littlehorizons.erp.controller.AcademicReportController;
import com.littlehorizons.erp.model.PerformanceReport;
import com.littlehorizons.erp.model.SubjectAverage;
import com.littlehorizons.erp.util.ConsoleInput;
import com.littlehorizons.erp.util.ReportFileWriter;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** Shows the academic performance report per subject in the console and saves it as a .txt file. */
public class AcademicReportView {

    private static final String LINE = "═".repeat(60);

    private final AcademicReportController controller;
    private final ConsoleInput input;

    public AcademicReportView(AcademicReportController controller, ConsoleInput input) {
        this.controller = controller;
        this.input = input;
    }

    public void show() {
        PerformanceReport report = controller.generateReport();
        if (report.isEmpty()) {
            System.out.println("\n" + LINE);
            System.out.println("No hay registros académicos suficientes para generar estadísticas.");
            System.out.println("Por favor, asegúrese de que existan notas registradas en el sistema.");
            System.out.println(LINE);
        } else {
            int year = LocalDate.now().getYear();
            String text = buildText(report, year);
            System.out.println("\n" + text);
            Path file = ReportFileWriter.save(Path.of("reports", "reporte_rendimiento_academico_" + year + ".txt"), text);
            System.out.println("Reporte generado exitosamente.");
            System.out.println("Archivo guardado en: " + file.toAbsolutePath());
        }
        input.readLine("Presione Enter para volver al menú");
    }

    /** Builds the whole report as plain text (the same text is printed and saved). */
    private String buildText(PerformanceReport report, int year) {
        StringBuilder text = new StringBuilder();
        text.append("╔").append("═".repeat(58)).append("╗\n");
        text.append(String.format("║ %-56s ║%n", "REPORTE DE RENDIMIENTO ACADÉMICO POR MATERIA - " + year));
        text.append("╚").append("═".repeat(58)).append("╝\n\n");

        text.append(LINE).append('\n');
        text.append("PROMEDIO GENERAL POR MATERIA (Ordenado de Mayor a Menor)\n");
        text.append(LINE).append("\n\n");
        text.append(report.ranking().stream()
                .map(s -> String.format("Código: %-8s | Materia: %-32s | Promedio: %s", s.subjectCode(),
                        s.subjectName(), s.average().toPlainString()))
                .collect(Collectors.joining("\n")));
        text.append("\n\n").append(LINE).append('\n');
        text.append("MATERIA(S) CON MEJOR RENDIMIENTO\n");
        text.append(LINE).append("\n\n");

        List<SubjectAverage> best = report.bestSubjects();
        if (best.size() == 1) {
            text.append("🏆 MATERIA DESTACADA: ").append(best.get(0).subjectName()).append('\n');
            text.append("   Promedio General: ").append(best.get(0).average().toPlainString()).append('\n');
            text.append("   Esta materia presenta el mejor rendimiento académico del colegio.\n");
        } else {
            text.append("🏆 MATERIAS DESTACADAS (Empate):\n\n");
            best.forEach(s -> text.append(String.format("   - %-32s | Promedio: %s%n", s.subjectName(),
                    s.average().toPlainString())));
            text.append("\n   Estas materias comparten el mejor rendimiento académico del colegio.\n");
        }
        text.append('\n').append(LINE).append('\n');
        return text.toString();
    }
}
