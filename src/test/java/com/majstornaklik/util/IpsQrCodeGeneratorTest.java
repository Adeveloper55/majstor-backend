package com.majstornaklik.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class IpsQrCodeGeneratorTest {

    @Test
    void buildsValidIpsPayloadWithModel97Checksum() {
        String payload = IpsQrCodeGenerator.buildPaymentPayload(
                "170-0050054399000-26",
                "USLUGE INFORMACIONE TEHNOLOGIJE KLIKO",
                new BigDecimal("12000.00"),
                "Uplata za tokene",
                "97",
                "365000001"
        );

        assertTrue(payload.startsWith("K:PR|V:01|C:1|"));
        assertTrue(payload.contains("R:170005005439900026|"));
        assertTrue(payload.contains("N:USLUGE INFORMACIONE TEHNOLOGIJE KLIKO|"));
        assertTrue(payload.contains("I:RSD12000,00|"));
        assertTrue(payload.contains("SF:221|"));
        assertTrue(payload.contains("S:Uplata za tokene|"));
        assertTrue(payload.contains("RO:9718365000001"));
    }

    @Test
    void model97ControlDigits() {
        assertEquals("9718365000001", IpsQrCodeGenerator.buildModel97Ro("365000001"));
    }

    @Test
    void formatPozivNaBrojDisplayModel97() {
        assertEquals("97-18365000001", IpsQrCodeGenerator.formatPozivNaBrojDisplay("97", "365000001"));
    }

    @Test
    void formatPozivNaBrojDisplayModel00() {
        assertEquals("365000001", IpsQrCodeGenerator.formatPozivNaBrojDisplay("00", "365000001"));
    }

    @Test
    void generatesPngBytes() throws Exception {
        String payload = IpsQrCodeGenerator.buildPaymentPayload(
                "170005005439900026",
                "USLUGE INFORMACIONE TEHNOLOGIJE KLIKO",
                new BigDecimal("22000"),
                "Uplata za tokene",
                "97",
                "365000001"
        );
        byte[] png = IpsQrCodeGenerator.generatePng(payload, 200);
        assertNotNull(png);
        assertTrue(png.length > 100);
        assertEquals((byte) 0x89, png[0]);
        assertEquals('P', png[1]);
        assertEquals('N', png[2]);
        assertEquals('G', png[3]);
    }

    @Test
    void rejectsInvalidAccountLength() {
        assertThrows(IllegalArgumentException.class, () ->
                IpsQrCodeGenerator.buildPaymentPayload(
                        "123",
                        "USLUGE INFORMACIONE TEHNOLOGIJE KLIKO",
                        BigDecimal.TEN,
                        "Uplata za tokene",
                        "97",
                        "365000001"
                ));
    }
}
