package com.majstornaklik.util;

import java.util.regex.Pattern;

public final class PhoneUtils {

    private static final Pattern SERBIAN_MOBILE = Pattern.compile("^\\+3816[0-9]{7,8}$");

    private PhoneUtils() {}

    public static String normalize(String phone) {
        if (phone == null) return null;
        String digits = phone.replaceAll("\\D", "");
        if (digits.isEmpty()) return null;

        String national = digits;
        if (national.startsWith("381")) {
            national = national.substring(3);
        } else if (national.startsWith("0")) {
            national = national.substring(1);
        }

        if (!national.startsWith("6")) {
            return null;
        }

        String normalized = "+381" + national;
        return SERBIAN_MOBILE.matcher(normalized).matches() ? normalized : null;
    }

    public static void validate(String phone) {
        if (normalize(phone) == null) {
            throw new IllegalArgumentException("Unesite ispravan srpski mobilni broj (npr. 0641234567).");
        }
    }

    public static String normalizeOptional(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String normalized = normalize(phone);
        if (normalized == null) {
            throw new IllegalArgumentException("Unesite ispravan srpski mobilni broj (npr. 0641234567).");
        }
        return normalized;
    }
}
