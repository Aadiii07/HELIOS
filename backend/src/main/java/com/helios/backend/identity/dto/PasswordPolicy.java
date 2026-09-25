package com.helios.backend.identity.dto;

/**
 * Single source of truth for the password policy regex, used by both
 * RegisterRequest and ChangePasswordRequest so the two can't silently
 * drift apart. Annotation attributes must be compile-time constants,
 * hence public static final rather than a method.
 */
public final class PasswordPolicy {

    public static final String REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{12,128}$";
    public static final String MESSAGE =
            "password must be at least 12 characters and include an uppercase letter, a lowercase letter, and a digit";

    private PasswordPolicy() {
    }
}
