package com.majstornaklik.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CityUtilsTest {

    @Test
    void matchesCityIgnoresDiacritics() {
        assertThat(CityUtils.matchesCity("Niš", "Nis")).isTrue();
        assertThat(CityUtils.matchesCity("Nis", "Niš")).isTrue();
    }

    @Test
    void matchesCityHandlesGradPrefix() {
        assertThat(CityUtils.matchesCity("Grad Niš", "Niš")).isTrue();
        assertThat(CityUtils.matchesCity("Grad Beograd", "Beograd")).isTrue();
    }

    @Test
    void matchesCityHandlesDistrictMapping() {
        assertThat(CityUtils.matchesCity("Nišavski", "Niš")).isTrue();
        assertThat(CityUtils.matchesCity("Južno-bački", "Novi Sad")).isTrue();
    }

    @Test
    void matchesCityInAddress() {
        assertThat(CityUtils.matchesCity("Nis, 18000 Niš", "Niš")).isTrue();
    }

    @Test
    void inferCityFromDistricts() {
        assertThat(CityUtils.inferCityFromDistricts(List.of("Grad Niš"))).contains("Nis");
        assertThat(CityUtils.inferCityFromDistricts(List.of("Nišavski"))).contains("Nis");
    }
}
