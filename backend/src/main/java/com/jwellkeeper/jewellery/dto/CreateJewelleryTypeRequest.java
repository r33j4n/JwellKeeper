package com.jwellkeeper.jewellery.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateJewelleryTypeRequest(
        @NotBlank @Size(max = 120)
        String name
) {
}
