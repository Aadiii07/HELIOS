package com.helios.backend.patient.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientProfileResponse(
        UUID id,
        UUID userId,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String phoneNumber,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String preferredLanguage,
        Instant createdAt,
        Instant updatedAt
) {
}
