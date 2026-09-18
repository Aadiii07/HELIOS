package com.helios.backend.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_SECRET_LENGTH = 32; // >= 256 bits for HS256

    private final String secret;
    private final long expirationMinutes;
    private SecretKey key;

    public JwtService(
            @Value("${security.jwt.secret:}") String secret,
            @Value("${security.jwt.expiration-minutes:60}") long expirationMinutes
    ) {
        this.secret = secret;
        this.expirationMinutes = expirationMinutes;
    }

    @PostConstruct
    void init() {
        // Fail fast rather than silently signing tokens with a weak/absent
        // secret (master spec §40, §49 — no hardcoded/default secrets).
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Generate one (e.g. `openssl rand -base64 32` or, on " +
                    "Windows PowerShell, `[Convert]::ToBase64String((1..32|%{Get-Random -Max 256}))`) " +
                    "and set it in backend/.env before starting the application.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + secret.getBytes(StandardCharsets.UTF_8).length +
                    " bytes). It must be at least " + MIN_SECRET_LENGTH + " bytes for HS256.");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String issueToken(UUID userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(expirationMinutes, ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public long expirationSeconds() {
        return expirationMinutes * 60;
    }

    /**
     * Parses and validates a token, returning its claims.
     * Throws {@link JwtException} (unchecked) if invalid/expired/tampered.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
