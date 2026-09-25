package com.helios.backend.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangePasswordRequest(

        @NotBlank
        String currentPassword,

        @NotBlank
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String newPassword
) {
    // Records auto-generate toString() from all fields by default —
    // override so a future accidental log/debug statement can never
    // print plaintext passwords (master spec §38: never log secrets).
    @Override
    public String toString() {
        return "ChangePasswordRequest[currentPassword=REDACTED, newPassword=REDACTED]";
    }
}
