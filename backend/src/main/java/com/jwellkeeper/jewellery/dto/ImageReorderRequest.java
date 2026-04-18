package com.jwellkeeper.jewellery.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record ImageReorderRequest(
        @NotEmpty List<UUID> imageIds
) {
}
