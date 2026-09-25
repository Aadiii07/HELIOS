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

        // See PasswordPolicy — shared with ChangePasswordRequest so the
        // two can't silently drift apart (master spec §13: "secure
        // password policy").
        @NotBlank
        @Pattern(regexp = PasswordPolicy.REGEX, message = PasswordPolicy.MESSAGE)
        String password,

        @NotNull
        @RegistrableRole
        Role role
) {
}
