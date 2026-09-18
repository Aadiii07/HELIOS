package com.helios.backend.identity.dto;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Public self-registration may only create PATIENT or PROVIDER accounts.
 * ORGANIZATION_ADMIN and SYSTEM_ADMIN are never trusted from client
 * input (master spec §13: "Never trust role information supplied by
 * the client") — those are provisioned through an admin-only path,
 * not yet implemented.
 */
@Documented
@Constraint(validatedBy = RegistrableRoleValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RegistrableRole {
    String message() default "role must be PATIENT or PROVIDER for self-registration";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
