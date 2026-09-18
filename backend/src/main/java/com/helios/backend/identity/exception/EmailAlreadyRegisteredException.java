package com.helios.backend.identity.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyRegisteredException extends ApiException {
    public EmailAlreadyRegisteredException() {
        super(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "An account with this email already exists");
    }
}
