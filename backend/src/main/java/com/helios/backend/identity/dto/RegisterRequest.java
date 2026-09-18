package com.helios.backend.identity.dto;

import com.helios.backend.identity.domain.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(

        @NotBlank
        @Email
        String email,

        // Minimum 12 characters, at least one uppercase, one lowercase,
        // one digit (master spec §13: "secure password policy").
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{12,128}$",
                message = "password must be at least 12 characters and include an uppercase letter, a lowercase letter, and a digit"
        )
        String password,

        @NotNull
        @RegistrableRole
        Role role
) {
}
