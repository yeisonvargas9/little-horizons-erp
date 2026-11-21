package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.DataAccessException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Saves a text report as a UTF-8 .txt file, creating the folder if needed. */
public final class ReportFileWriter {

    private ReportFileWriter() {
    }

    public static Path save(Path file, String content) {
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            return Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            AppLogger.error("Could not save the report " + file, e);
            throw new DataAccessException("No fue posible guardar el reporte en " + file + ".", e);
        }
    }
}
