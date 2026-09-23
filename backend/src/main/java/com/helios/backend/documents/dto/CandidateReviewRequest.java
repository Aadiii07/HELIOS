package com.helios.backend.documents.dto;

import com.helios.backend.documents.domain.ReviewStatus;
import jakarta.validation.constraints.NotNull;

/**
 * reviewStatus must be CONFIRMED, REJECTED, or CORRECTED — PENDING is
 * the initial state only and is rejected here (see DocumentExtractionService).
 * correctedFieldLabel/correctedRawValue are required only when
 * reviewStatus is CORRECTED; validated in the service rather than via
 * annotations here, since the requirement is conditional on another
 * field's value.
 */
public record CandidateReviewRequest(
        @NotNull ReviewStatus reviewStatus,
        String correctedFieldLabel,
        String correctedRawValue,
        String correctedUnit
) {
}
