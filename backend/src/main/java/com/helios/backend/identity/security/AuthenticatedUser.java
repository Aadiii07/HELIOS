package com.helios.backend.identity.security;

import java.util.UUID;

/**
 * The authenticated principal placed in the SecurityContext by
 * {@link JwtAuthenticationFilter}. Controllers/services access this
 * via {@code @AuthenticationPrincipal} rather than re-parsing the token.
 */
public record AuthenticatedUser(UUID id, String email, String role) {
}
