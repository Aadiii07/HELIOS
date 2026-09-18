package com.helios.backend.common.error;

import org.springframework.http.HttpStatus;

/**
 * Base for exceptions that should be translated into the standard
 * API error response (see {@link ApiError}) by {@link GlobalExceptionHandler}.
 * Module-specific exceptions extend this instead of each module
 * writing its own {@code @ExceptionHandler}.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
