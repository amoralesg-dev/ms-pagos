package com.rassini.pagos.util;

/**
 * Enmascarado de datos bancarios sensibles: conserva los ultimos 4 caracteres y reemplaza el resto por '*'.
 */
public final class BankDataMasker {

    private static final int VISIBLE = 4;

    private BankDataMasker() {
    }

    public static String mask(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.length() <= VISIBLE) {
            return "*".repeat(v.length());
        }
        return "*".repeat(v.length() - VISIBLE) + v.substring(v.length() - VISIBLE);
    }
}
