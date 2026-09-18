package com.helios.backend.identity.dto;

import com.helios.backend.identity.domain.AccountStatus;
import com.helios.backend.identity.domain.Role;

import java.time.Instant;
import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        Role role,
        AccountStatus accountStatus,
        Instant createdAt
) {
}
