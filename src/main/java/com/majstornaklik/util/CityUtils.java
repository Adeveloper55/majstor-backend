package com.majstornaklik.util;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class CityUtils {

    private static final Map<String, String> DISTRICT_TO_CITY = Map.ofEntries(
            Map.entry("grad beograd", "beograd"),
            Map.entry("grad nis", "nis"),
            Map.entry("nisavski", "nis"),
            Map.entry("toplicki", "nis"),
            Map.entry("pirotski", "pirot"),
            Map.entry("juzno banatski", "pancevo"),
            Map.entry("juzno backi", "novi sad"),
            Map.entry("severno banatski", "zrenjanin"),
            Map.entry("severno backi", "subotica"),
            Map.entry("srednje banatski", "zrenjanin"),
            Map.entry("zapadno backi", "sombor"),
            Map.entry("sremski", "sremska mitrovica"),
            Map.entry("sumadijski", "kragujevac"),
            Map.entry("podunavski", "smederevo"),
            Map.entry("branicevski", "pozarevac"),
            Map.entry("borski", "bor"),
            Map.entry("zajecarski", "zajecar"),
            Map.entry("pčinjski", "vranje"),
            Map.entry("pcinjski", "vranje"),
            Map.entry("jablanički", "leskovac"),
            Map.entry("jablanski", "leskovac"),
            Map.entry("rasinski", "krusevac"),
            Map.entry("moravicki", "cacak"),
            Map.entry("zlatiborski", "uzice")
    );

    private CityUtils() {}

    public static boolean matchesCity(String storedValue, String filterCity) {
        if (filterCity == null || filterCity.isBlank()) {
            return true;
        }
        if (storedValue == null || storedValue.isBlank()) {
            return false;
        }

        String filter = normalizeCity(filterCity);
        if (filter.isEmpty()) {
            return true;
        }

        String stored = normalizeCity(storedValue);
        if (matchesNormalized(stored, filter)) {
            return true;
        }

        String withoutGrad = stripGradPrefix(stored);
        if (!withoutGrad.equals(stored) && matchesNormalized(withoutGrad, filter)) {
            return true;
        }

        String mappedCity = DISTRICT_TO_CITY.get(stored);
        if (mappedCity != null && matchesNormalized(mappedCity, filter)) {
            return true;
        }

        mappedCity = DISTRICT_TO_CITY.get(withoutGrad);
        return mappedCity != null && matchesNormalized(mappedCity, filter);
    }

    public static Optional<String> inferCityFromDistricts(List<String> districts) {
        if (districts == null || districts.isEmpty()) {
            return Optional.empty();
        }
        for (String district : districts) {
            if (district == null || district.isBlank()) {
                continue;
            }
            String normalized = normalizeCity(district);
            String mapped = DISTRICT_TO_CITY.get(normalized);
            if (mapped != null) {
                return Optional.of(displayCity(mapped));
            }
            String withoutGrad = stripGradPrefix(normalized);
            if (withoutGrad.startsWith("grad ")) {
                withoutGrad = withoutGrad.substring(5).trim();
            }
            if (!withoutGrad.isBlank() && !withoutGrad.equals(normalized)) {
                return Optional.of(displayCity(withoutGrad));
            }
        }
        return Optional.empty();
    }

    public static String normalizeCity(String city) {
        if (city == null) {
            return "";
        }
        String normalized = Normalizer.normalize(city.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace("đ", "dj");
        return normalized.replaceAll("[^a-z0-9\\s-]", "")
                .replace('-', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static boolean matchesNormalized(String stored, String filter) {
        if (stored.isEmpty() || filter.isEmpty()) {
            return false;
        }
        return stored.equals(filter) || stored.contains(filter) || filter.contains(stored);
    }

    private static String stripGradPrefix(String normalized) {
        if (normalized.startsWith("grad ")) {
            return normalized.substring(5).trim();
        }
        return normalized;
    }

    private static String displayCity(String normalized) {
        if (normalized.isEmpty()) {
            return normalized;
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }
}
