package com.majstornaklik.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

/**
 * NBS IPS QR (K:PR) za domaće plaćanje — specifikacija NBS IPS QR kôd PR.
 */
public final class IpsQrCodeGenerator {

    private IpsQrCodeGenerator() {
    }

    public static String buildPaymentPayload(
            String bankAccount,
            String recipientName,
            BigDecimal amountRsd,
            String paymentPurpose,
            String paymentModel,
            String pozivNaBroj
    ) {
        String account = digitsOnly(bankAccount);
        if (account.length() != 18) {
            throw new IllegalArgumentException("Broj računa mora imati 18 cifara.");
        }

        String name = truncate(recipientName != null ? recipientName.trim() : "Primalac", 70);
        String purpose = truncate(paymentPurpose != null ? paymentPurpose.trim() : "Uplata", 35);
        String amount = formatRsdAmount(amountRsd);

        StringBuilder payload = new StringBuilder();
        payload.append("K:PR|V:01|C:1|");
        payload.append("R:").append(account).append("|");
        payload.append("N:").append(name).append("|");
        payload.append("I:RSD").append(amount).append("|");
        payload.append("SF:221|");
        payload.append("S:").append(purpose);

        String ro = buildRoTag(paymentModel, pozivNaBroj);
        if (!ro.isBlank()) {
            payload.append("|RO:").append(ro);
        }

        return payload.toString();
    }

    /** RO tag vrednost za NBS IPS QR (bez prefiksa "RO:"). */
    public static String buildRoTag(String paymentModel, String pozivNaBroj) {
        String referenceDigits = pozivNaBroj != null ? pozivNaBroj.replaceAll("\\D", "") : "";
        if (referenceDigits.isBlank()) {
            return "";
        }

        String model = paymentModel != null ? paymentModel.replaceAll("\\D", "") : "";
        if (model.isBlank()) {
            model = "00";
        }

        if ("97".equals(model)) {
            return buildModel97Ro(referenceDigits);
        }

        if ("00".equals(model)) {
            return "00" + referenceDigits;
        }

        return model + referenceDigits;
    }

    /** Model 97: ISO 7064 MOD 97,10 kontrolni broj + osnovni poziv na broj. */
    static String buildModel97Ro(String baseReference) {
        if (baseReference.isBlank()) {
            return "";
        }
        BigInteger number = new BigInteger(baseReference + "00");
        int remainder = number.mod(BigInteger.valueOf(97)).intValue();
        int check = 98 - remainder;
        if (check == 98) {
            check = 0;
        }
        return "97" + String.format("%02d", check) + baseReference;
    }

    /** Za prikaz u emailu / UI. */
    public static String formatPozivNaBrojDisplay(String model, String reference) {
        String digits = reference != null ? reference.replaceAll("\\D", "") : "";
        if (digits.isBlank()) {
            return "";
        }
        String m = model != null && !model.isBlank() ? model.replaceAll("\\D", "") : "00";
        if ("97".equals(m)) {
            String ro = buildModel97Ro(digits);
            return "97-" + ro.substring(2);
        }
        if ("00".equals(m)) {
            return digits;
        }
        return m + "-" + digits;
    }

    public static byte[] generatePng(String payload, int sizePx) throws WriterException, IOException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }

    private static String formatRsdAmount(BigDecimal amountRsd) {
        BigDecimal normalized = amountRsd.setScale(2, RoundingMode.HALF_UP);
        return normalized.toPlainString().replace('.', ',');
    }

    private static String digitsOnly(String value) {
        return value.replaceAll("\\D", "");
    }

    private static String truncate(String value, int maxLen) {
        if (value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen);
    }
}
