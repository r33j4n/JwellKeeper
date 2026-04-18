package com.jwellkeeper.jewellery.dto;

public record QrImageResponse(
        String contentType,
        String qrCodeBase64
) {
}
