package com.helios.backend.identity.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

public class AccountNotActiveException extends ApiException {
    public AccountNotActiveException() {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "This account is not active");
    }
}
