package com.jwellkeeper.jewellery.dto;

import java.util.UUID;

public record JewelleryTypeResponse(
        UUID id,
        String name,
        boolean custom
) {
}
