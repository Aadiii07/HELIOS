package com.helios.backend.patient.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record PatientProfileRequest(

        @NotBlank
        @Size(max = 100)
        String firstName,

        @NotBlank
        @Size(max = 100)
        String lastName,

        @NotNull
        @Past
        LocalDate dateOfBirth,

        // Permissive on purpose: international formats vary widely
        // (spaces, dashes, parens, extensions). Just enough to catch
        // garbage input, not to validate a specific country's format.
        @Pattern(regexp = "^[0-9+()\\-\\s]{7,32}$", message = "must be a plausible phone number")
        String phoneNumber,

        @Size(max = 255)
        String addressLine1,

        @Size(max = 255)
        String addressLine2,

        @Size(max = 100)
        String city,

        @Size(max = 100)
        String state,

        @Size(max = 20)
        String postalCode,

        @Size(max = 100)
        String country,

        @Size(max = 10)
        String preferredLanguage
) {
}
