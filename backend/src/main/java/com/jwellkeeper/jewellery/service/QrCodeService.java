package com.jwellkeeper.jewellery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.EncodeHintType;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.jewellery.dto.QrPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.EnumMap;
import java.util.Map;
import java.util.Base64;

@Service
public class QrCodeService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String NEW_TOKEN_PREFIX = "v2_";
    private static final int NEW_TOKEN_BYTES = 15;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final int imageSize;

    public QrCodeService(
            ObjectMapper objectMapper,
            @Value("${app.qr.secret}") String secret,
            @Value("${app.qr.image-size}") int imageSize
    ) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.imageSize = imageSize;
    }

    public String createToken(QrPayload payload) {
        return NEW_TOKEN_PREFIX + base64Url(randomBytes(NEW_TOKEN_BYTES));
    }

    public QrPayload verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new BadRequestException("Invalid QR token");
            }
            String expected = sign(parts[0]);
            if (!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[1].getBytes(StandardCharsets.UTF_8))) {
                throw new BadRequestException("Invalid QR signature");
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            return objectMapper.readValue(payloadBytes, QrPayload.class);
        } catch (BadRequestException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BadRequestException("Invalid QR token");
        }
    }

    public String generateBase64Png(String token) {
        try {
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(generatePngBytes(token));
        } catch (Exception ex) {
            throw new BadRequestException("Unable to generate QR image");
        }
    }

    public byte[] generatePngBytes(String token) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            hints.put(EncodeHintType.MARGIN, 1);
            if (token != null && token.startsWith(NEW_TOKEN_PREFIX)) {
                hints.put(EncodeHintType.QR_VERSION, 2);
            }
            BitMatrix matrix = writer.encode(token, BarcodeFormat.QR_CODE, imageSize, imageSize, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | java.io.IOException ex) {
            throw new BadRequestException("Unable to generate QR image");
        }
    }

    private byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }

    private String sign(String encodedPayload) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return base64Url(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
