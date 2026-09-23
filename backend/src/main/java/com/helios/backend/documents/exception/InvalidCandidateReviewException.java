package com.helios.backend.documents.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidCandidateReviewException extends ApiException {
    public InvalidCandidateReviewException(String message) {
        super(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }
}
