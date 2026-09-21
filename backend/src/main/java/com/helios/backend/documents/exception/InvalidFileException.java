package com.helios.backend.documents.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class InvalidFileException extends ApiException {
    public InvalidFileException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_FILE", message);
    }
}
