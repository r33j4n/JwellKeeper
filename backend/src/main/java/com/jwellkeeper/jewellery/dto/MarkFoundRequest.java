package com.jwellkeeper.jewellery.dto;

import jakarta.validation.constraints.NotBlank;

public record MarkFoundRequest(@NotBlank String ownerPassword) {
}
