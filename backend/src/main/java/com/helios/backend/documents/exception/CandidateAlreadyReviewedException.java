package com.helios.backend.documents.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * A candidate can only transition out of PENDING once. Without this,
 * reviewing the same candidate twice (e.g. CONFIRM then later
 * REJECT) would create a second, duplicate Observation — see
 * ObservationService integration in DocumentExtractionService.
 */
public class CandidateAlreadyReviewedException extends ApiException {
    public CandidateAlreadyReviewedException() {
        super(HttpStatus.CONFLICT, "CANDIDATE_ALREADY_REVIEWED", "This candidate has already been reviewed");
    }
}
