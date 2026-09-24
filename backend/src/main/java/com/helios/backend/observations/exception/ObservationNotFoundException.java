package com.helios.backend.observations.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class ObservationNotFoundException extends ApiException {
    public ObservationNotFoundException() {
        super(HttpStatus.NOT_FOUND, "OBSERVATION_NOT_FOUND", "No observation found with that ID");
    }
}
