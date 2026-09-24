package com.helios.backend.observations.dto;

import com.helios.backend.documents.domain.Confidence;
import com.helios.backend.observations.domain.ObservationSource;
import com.helios.backend.observations.domain.ObservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ObservationResponse(
        UUID id,
        String code,
        String displayName,
        String rawValue,
        Double numericValue,
        String unit,
        String referenceRange,
        LocalDate effectiveDate,
        ObservationSource source,
        UUID sourceDocumentId,
        UUID sourceCandidateId,
        Confidence confidence,
        ObservationStatus status,
        Instant createdAt
) {
}
