package com.helios.backend.patient.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class ProfileNotFoundException extends ApiException {
    public ProfileNotFoundException() {
        super(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "No patient profile exists for this account yet");
    }
}
