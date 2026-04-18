package com.jwellkeeper.users.dto;

import com.jwellkeeper.users.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Size(max = 140)
        String name,

        @NotBlank @Email @Size(max = 180)
        String email,

        @NotBlank @Size(min = 8, max = 100)
        String password,

        UserRole role
) {
}
