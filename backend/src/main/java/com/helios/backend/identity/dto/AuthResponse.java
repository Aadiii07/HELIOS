package com.helios.backend.identity.dto;

import com.helios.backend.identity.domain.Role;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UUID userId,
        Role role
) {
}
