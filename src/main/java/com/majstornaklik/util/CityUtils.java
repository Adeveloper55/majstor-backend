package com.majstornaklik.util;

import java.text.Normalizer;
import java.util.Locale;

public final class CityUtils {

    private CityUtils() {}

    public static boolean matchesCity(String jobCity, String filterCity) {
        if (filterCity == null || filterCity.isBlank()) {
            return true;
        }
        if (jobCity == null || jobCity.isBlank()) {
            return false;
        }
        String job = normalizeCity(jobCity);
        String filter = normalizeCity(filterCity);
        return job.equals(filter) || job.contains(filter) || filter.contains(job);
    }

    public static String normalizeCity(String city) {
        if (city == null) {
            return "";
        }
        String normalized = Normalizer.normalize(city.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "dj");
        return normalized.replaceAll("[^a-z0-9\\s-]", "").trim();
    }
}
