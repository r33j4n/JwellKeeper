package com.jwellkeeper.jewellery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.jewellery.dto.QrPayload;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrCodeServiceTest {

    private final QrCodeService service = new QrCodeService(
            new ObjectMapper(),
            "test-qr-signing-secret-with-enough-length",
            150
    );

    @Test
    void createsShortQrToken() {
        String token = service.createToken(new QrPayload(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(token).startsWith("v2_");
        assertThat(token).hasSizeLessThanOrEqualTo(25);
    }

    @Test
    void verifiesLegacySignedQrPayload() {
        UUID jewelleryId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        QrPayload payload = new QrPayload(jewelleryId, tenantId);

        String token = legacyToken(payload);
        QrPayload resolved = service.verify(token);

        assertThat(resolved.jewelleryId()).isEqualTo(jewelleryId);
        assertThat(resolved.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void rejectsTamperedLegacyQrPayload() {
        String token = legacyToken(new QrPayload(UUID.randomUUID(), UUID.randomUUID()));

        assertThatThrownBy(() -> service.verify(token + "tampered"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid QR");
    }

    @Test
    void generatesBase64Png() {
        String token = service.createToken(new QrPayload(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(service.generateBase64Png(token)).startsWith("data:image/png;base64,");
    }

    private String legacyToken(QrPayload payload) {
        try {
            String payloadJson = new ObjectMapper().writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec("test-qr-signing-secret-with-enough-length".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(encodedPayload.getBytes(StandardCharsets.UTF_8)));
            return encodedPayload + "." + signature;
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
