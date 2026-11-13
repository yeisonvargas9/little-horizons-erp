package com.littlehorizons.erp.util;

import com.littlehorizons.erp.exception.OperationCancelledException;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Validated console reader. Every method repeats the question until the answer is valid,
 * and typing the cancel keyword at any prompt throws {@link OperationCancelledException}.
 */
public class ConsoleInput {

    public static final String CANCEL_KEYWORD = "cancelar";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final Scanner scanner;

    public ConsoleInput(InputStream stream) {
        this.scanner = new Scanner(stream, StandardCharsets.UTF_8);
    }

    public String readLine(String prompt) {
        System.out.print(prompt + ": ");
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("Console input closed");
        }
        String line = scanner.nextLine().trim();
        if (CANCEL_KEYWORD.equalsIgnoreCase(line)) {
            throw new OperationCancelledException();
        }
        return line;
    }

    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("  ✖ Este dato es obligatorio.");
        }
    }

    /** Returns null when the user just presses Enter. */
    public String readOptional(String prompt) {
        String value = readLine(prompt);
        return value.isEmpty() ? null : value;
    }

    public String readValidated(String prompt, Predicate<String> rule, String errorMessage) {
        while (true) {
            String value = readRequired(prompt);
            if (rule.test(value)) {
                return value;
            }
            System.out.println("  ✖ " + errorMessage);
        }
    }

    /** Returns null when the user presses Enter; otherwise the value must satisfy the rule. */
    public String readOptionalValidated(String prompt, Predicate<String> rule, String errorMessage) {
        while (true) {
            String value = readOptional(prompt);
            if (value == null || rule.test(value)) {
                return value;
            }
            System.out.println("  ✖ " + errorMessage);
        }
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String value = readRequired(prompt);
            try {
                int number = Integer.parseInt(value);
                if (number >= min && number <= max) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // falls through to the error message below
            }
            System.out.printf("  ✖ Ingrese un número entre %d y %d.%n", min, max);
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String value = readRequired(prompt);
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  ✖ Fecha inválida. Use el formato AAAA-MM-DD (ej: 2015-03-27).");
            }
        }
    }

    /** Returns null when the user presses Enter. */
    public LocalDate readOptionalDate(String prompt) {
        while (true) {
            String value = readOptional(prompt);
            if (value == null) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  ✖ Fecha inválida. Use el formato AAAA-MM-DD (ej: 2026-11-10).");
            }
        }
    }

    public LocalTime readTime(String prompt) {
        while (true) {
            String value = readRequired(prompt);
            try {
                return LocalTime.parse(value, TIME_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("  ✖ Hora inválida. Use el formato HH:mm (ej: 07:30).");
            }
        }
    }

    public <T> T readChoice(String title, T[] options, Function<T, String> labelProvider) {
        System.out.println(title + ":");
        for (int i = 0; i < options.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, labelProvider.apply(options[i]));
        }
        return options[readInt("Seleccione una opción", 1, options.length) - 1];
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String value = readRequired(prompt + " (s/n)").toLowerCase();
            if (value.equals("s") || value.equals("si") || value.equals("sí")) {
                return true;
            }
            if (value.equals("n") || value.equals("no")) {
                return false;
            }
            System.out.println("  ✖ Responda 's' o 'n'.");
        }
    }
}
