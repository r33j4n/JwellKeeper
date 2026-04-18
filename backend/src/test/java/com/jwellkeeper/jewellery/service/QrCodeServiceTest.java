package com.jwellkeeper.jewellery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jwellkeeper.common.exception.BadRequestException;
import com.jwellkeeper.jewellery.dto.QrPayload;
import org.junit.jupiter.api.Test;

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
    void createsAndVerifiesSignedQrPayload() {
        UUID jewelleryId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = service.createToken(new QrPayload(jewelleryId, tenantId));
        QrPayload resolved = service.verify(token);

        assertThat(resolved.jewelleryId()).isEqualTo(jewelleryId);
        assertThat(resolved.tenantId()).isEqualTo(tenantId);
    }

    @Test
    void rejectsTamperedQrPayload() {
        String token = service.createToken(new QrPayload(UUID.randomUUID(), UUID.randomUUID()));

        assertThatThrownBy(() -> service.verify(token + "tampered"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid QR");
    }

    @Test
    void generatesBase64Png() {
        String token = service.createToken(new QrPayload(UUID.randomUUID(), UUID.randomUUID()));

        assertThat(service.generateBase64Png(token)).startsWith("data:image/png;base64,");
    }
}
