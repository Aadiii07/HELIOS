package com.helios.backend.documents.dto;

import com.helios.backend.documents.domain.Confidence;
import com.helios.backend.documents.domain.ReviewStatus;

import java.time.Instant;
import java.util.UUID;

public record CandidateResponse(
        UUID id,
        UUID documentId,
        String fieldLabel,
        String rawValue,
        String unit,
        String referenceRange,
        String sourceExcerpt,
        Confidence confidence,
        String extractionMethod,
        ReviewStatus reviewStatus,
        String correctedFieldLabel,
        String correctedRawValue,
        String correctedUnit,
        Instant reviewedAt,
        Instant createdAt
) {
}
