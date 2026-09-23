package com.helios.backend.documents.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class CandidateNotFoundException extends ApiException {
    public CandidateNotFoundException() {
        super(HttpStatus.NOT_FOUND, "CANDIDATE_NOT_FOUND", "No extraction candidate found with that ID");
    }
}
