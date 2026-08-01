package com.timemark.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class QrCodeService {

    private static final String HMAC_ALGO = "HmacSHA256";

    @Value("${app.qr.secret}")
    private String secret;

    /** Token format: "OFFICE_CHECKIN:{date}:{hmac}" — rotates automatically every day. */
    public String generateDailyToken() {
        String date = LocalDate.now().toString();
        String payload = "OFFICE_CHECKIN:" + date;
        return payload + ":" + sign(payload);
    }

    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;
        String[] parts = token.split(":");
        if (parts.length != 3 || !parts[0].equals("OFFICE_CHECKIN")) return false;

        String date = parts[1];
        String providedSignature = parts[2];
        String payload = parts[0] + ":" + parts[1];

        if (!date.equals(LocalDate.now().toString())) return false; // expired — new day, new code
        return sign(payload).equals(providedSignature);
    }

    /** Renders the given token as a QR code PNG, returned as a base64 data URL. */
    public String toBase64Png(String token) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(token, BarcodeFormat.QR_CODE, 280, 280);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("Failed to generate QR code", e);
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] result = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign QR token", e);
        }
    }
}
