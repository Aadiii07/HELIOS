package com.helios.backend.identity.dto;

import com.helios.backend.identity.domain.Role;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RegistrableRoleValidator implements ConstraintValidator<RegistrableRole, Role> {

    @Override
    public boolean isValid(Role role, ConstraintValidatorContext context) {
        return role == Role.PATIENT || role == Role.PROVIDER;
    }
}
