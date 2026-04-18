package com.jwellkeeper.jewellery.dto;

import java.util.UUID;

public record QrPayload(UUID jewelleryId, UUID tenantId) {
}
