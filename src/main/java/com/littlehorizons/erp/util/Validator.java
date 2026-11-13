package com.littlehorizons.erp.util;

import java.util.regex.Pattern;

/** Reusable input rules (pure functions, no state). */
public final class Validator {

    private static final Pattern DOCUMENT = Pattern.compile("[A-Za-z0-9]{5,20}");
    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L} '.-]{0,98}");
    private static final Pattern PHONE = Pattern.compile("\\+?\\d{7,15}");
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern BLOOD_TYPE = Pattern.compile("(A|B|AB|O)[+-]");

    private Validator() {
    }

    public static boolean isValidDocument(String value) {
        return value != null && DOCUMENT.matcher(value).matches();
    }

    public static boolean isValidName(String value) {
        return value != null && NAME.matcher(value).matches();
    }

    public static boolean isValidPhone(String value) {
        return value != null && PHONE.matcher(value).matches();
    }

    public static boolean isValidEmail(String value) {
        return value != null && EMAIL.matcher(value).matches();
    }

    public static boolean isValidBloodType(String value) {
        return value != null && BLOOD_TYPE.matcher(value).matches();
    }
}
