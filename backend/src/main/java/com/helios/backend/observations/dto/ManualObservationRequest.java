package com.helios.backend.observations.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ManualObservationRequest(

        @NotBlank
        @Size(max = 255)
        String displayName,

        @NotBlank
        @Size(max = 255)
        String value,

        @Size(max = 50)
        String unit,

        @Size(max = 100)
        String referenceRange,

        @NotNull
        @PastOrPresent
        LocalDate effectiveDate
) {
}
