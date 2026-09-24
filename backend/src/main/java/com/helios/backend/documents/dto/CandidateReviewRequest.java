package com.helios.backend.documents.dto;

import com.helios.backend.documents.domain.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

/**
 * reviewStatus must be CONFIRMED, REJECTED, or CORRECTED — PENDING is
 * the initial state only and is rejected here (see DocumentExtractionService).
 * correctedFieldLabel/correctedRawValue are required only when
 * reviewStatus is CORRECTED; validated in the service rather than via
 * annotations here, since the requirement is conditional on another
 * field's value.
 *
 * effectiveDate is used only when the review results in an
 * Observation (CONFIRMED/CORRECTED) — when omitted, the document's
 * upload date is used as a fallback (see DocumentExtractionService),
 * which is often NOT the same as the actual specimen/collection date
 * (master spec §57) — the patient can override it here.
 */
public record CandidateReviewRequest(
        @NotNull ReviewStatus reviewStatus,
        String correctedFieldLabel,
        String correctedRawValue,
        String correctedUnit,
        @PastOrPresent LocalDate effectiveDate
) {
}
