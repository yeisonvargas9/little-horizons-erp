package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Application logger. Writes every event to "error_log.txt" (project root) using java.util.logging,
 * so errors never clutter the console that the user is working with.
 */
public final class AppLogger {

    private static final String LOG_FILE = "error_log.txt";
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger LOGGER = Logger.getLogger("littlehorizons.erp");

    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.setLevel(Level.INFO);
        try {
            FileHandler handler = new FileHandler(LOG_FILE, true);
            handler.setEncoding("UTF-8");
            handler.setFormatter(new Formatter() {
                @Override
                public String format(LogRecord logRecord) {
                    StringBuilder line = new StringBuilder()
                            .append('[').append(LocalDateTime.now().format(TIMESTAMP)).append("] ")
                            .append(logRecord.getLevel()).append(" - ")
                            .append(formatMessage(logRecord)).append(System.lineSeparator());
                    if (logRecord.getThrown() != null) {
                        StringWriter trace = new StringWriter();
                        logRecord.getThrown().printStackTrace(new PrintWriter(trace));
                        line.append(trace).append(System.lineSeparator());
                    }
                    return line.toString();
                }
            });
            LOGGER.addHandler(handler);
        } catch (IOException e) {
            System.err.println("Could not open log file " + LOG_FILE + ": " + e.getMessage());
        }
    }

    private AppLogger() {
    }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warning(message);
    }

    public static void error(String message, Throwable cause) {
        LOGGER.log(Level.SEVERE, message, cause);
    }
}
