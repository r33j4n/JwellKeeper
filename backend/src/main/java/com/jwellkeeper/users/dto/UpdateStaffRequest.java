package com.jwellkeeper.users.dto;

import com.jwellkeeper.users.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateStaffRequest(
        @Size(max = 140)
        String name,

        @Email @Size(max = 180)
        String email,

        @Size(min = 8, max = 100)
        String password,

        Boolean active,

        UserRole role
) {
}
