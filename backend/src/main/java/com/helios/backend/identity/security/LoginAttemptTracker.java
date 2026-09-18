package com.helios.backend.identity.security;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory failed-login tracking, keyed by (lowercased) email.
 *
 * Deliberately simple for MVP scope (master spec §50, no
 * overengineering): single JVM instance, no persistence, no
 * distribution. Sufficient for one backend instance; a real deployment
 * with multiple instances needs a shared store (Redis) instead — noted
 * as a P1 backlog item, not built here.
 */
@Component
public class LoginAttemptTracker {

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L; // 15 minutes

    private record Attempts(int count, long windowStart) {
    }

    private final ConcurrentHashMap<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        Attempts current = attemptsByEmail.get(key(email));
        if (current == null) {
            return;
        }
        if (isWithinWindow(current) && current.count() >= MAX_ATTEMPTS) {
            throw new com.helios.backend.identity.exception.TooManyLoginAttemptsException();
        }
    }

    public void recordFailure(String email) {
        attemptsByEmail.compute(key(email), (k, existing) -> {
            if (existing == null || !isWithinWindow(existing)) {
                return new Attempts(1, Instant.now().toEpochMilli());
            }
            return new Attempts(existing.count() + 1, existing.windowStart());
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(key(email));
    }

    private boolean isWithinWindow(Attempts attempts) {
        return Instant.now().toEpochMilli() - attempts.windowStart() < WINDOW_MILLIS;
    }

    private String key(String email) {
        return email.toLowerCase();
    }
}
