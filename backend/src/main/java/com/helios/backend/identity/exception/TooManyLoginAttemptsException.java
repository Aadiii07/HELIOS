package com.helios.backend.identity.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class TooManyLoginAttemptsException extends ApiException {
    public TooManyLoginAttemptsException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_ATTEMPTS", "Too many failed login attempts. Try again later.");
    }
}
