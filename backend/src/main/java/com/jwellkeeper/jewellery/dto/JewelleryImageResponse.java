package com.jwellkeeper.jewellery.dto;

import com.jwellkeeper.jewellery.model.ImageCaptureSource;

import java.time.Instant;
import java.util.UUID;

public record JewelleryImageResponse(
        UUID id,
        UUID jewelleryId,
        ImageCaptureSource captureSource,
        String mimeType,
        long fileSizeBytes,
        Integer width,
        Integer height,
        String checksumSha256,
        int sortOrder,
        boolean primary,
        String url,
        Instant createdAt
) {
}
