package com.jwellkeeper.jewellery.dto;

import jakarta.validation.constraints.NotBlank;

public record QrResolveRequest(@NotBlank String token) {
}
