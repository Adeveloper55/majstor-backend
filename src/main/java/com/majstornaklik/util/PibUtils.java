package com.majstornaklik.util;

public final class PibUtils {

    private PibUtils() {}

    public static String normalize(String pib) {
        if (pib == null) return null;
        String digits = pib.replaceAll("\\D", "");
        return digits.isEmpty() ? null : digits;
    }

    public static void validate(String pib) {
        String normalized = normalize(pib);
        if (normalized == null) {
            throw new IllegalArgumentException("PIB je obavezan.");
        }
        if (normalized.length() != 9) {
            throw new IllegalArgumentException("PIB mora imati 9 cifara.");
        }
    }

    public static String normalizeOptional(String pib) {
        String normalized = normalize(pib);
        if (normalized != null) {
            validate(normalized);
        }
        return normalized;
    }
}
