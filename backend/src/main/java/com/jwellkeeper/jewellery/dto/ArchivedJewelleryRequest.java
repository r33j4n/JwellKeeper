package com.jwellkeeper.jewellery.dto;

import java.util.UUID;

public record ArchivedJewelleryRequest(
        String ownerPassword,
        UUID typeId,
        String karat,
        String q
) {
}
