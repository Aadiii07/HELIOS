package com.helios.backend.identity.service;

import com.helios.backend.common.audit.AuditActions;
import com.helios.backend.common.audit.AuditResults;
import com.helios.backend.common.audit.AuditService;
import com.helios.backend.identity.domain.AccountStatus;
import com.helios.backend.identity.domain.User;
import com.helios.backend.identity.dto.AuthResponse;
import com.helios.backend.identity.dto.LoginRequest;
import com.helios.backend.identity.dto.RegisterRequest;
import com.helios.backend.identity.dto.UserSummaryResponse;
import com.helios.backend.identity.exception.AccountNotActiveException;
import com.helios.backend.identity.exception.EmailAlreadyRegisteredException;
import com.helios.backend.identity.repository.UserRepository;
import com.helios.backend.identity.security.JwtService;
import com.helios.backend.identity.security.LoginAttemptTracker;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptTracker loginAttemptTracker;
    private final AuditService auditService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            LoginAttemptTracker loginAttemptTracker,
            AuditService auditService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.loginAttemptTracker = loginAttemptTracker;
        this.auditService = auditService;
    }

    @Transactional
    public UserSummaryResponse register(RegisterRequest request, String ipAddress) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            auditService.record(null, email, AuditActions.REGISTER, "User", null, AuditResults.FAILURE, ipAddress);
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.role());
        user = userRepository.save(user);

        auditService.record(user.getId(), email, AuditActions.REGISTER, "User", user.getId().toString(), AuditResults.SUCCESS, ipAddress);

        return new UserSummaryResponse(user.getId(), user.getEmail(), user.getRole(), user.getAccountStatus(), user.getCreatedAt());
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        String email = normalizeEmail(request.email());

        loginAttemptTracker.checkAllowed(email);

        User user = userRepository.findByEmail(email).orElse(null);

        // Constant-shape failure path: don't reveal whether the email
        // exists. Always run a hash comparison even on a missing user
        // (against a fixed dummy hash) so response timing doesn't leak
        // account existence.
        boolean passwordMatches = user != null && passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!passwordMatches) {
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            loginAttemptTracker.recordFailure(email);
            auditService.record(user != null ? user.getId() : null, email, AuditActions.LOGIN_FAILED, "User", null, AuditResults.FAILURE, ipAddress);
            throw new BadCredentialsException("Invalid credentials");
        }

        if (user.getAccountStatus() != AccountStatus.ACTIVE) {
            auditService.record(user.getId(), email, AuditActions.LOGIN_FAILED, "User", user.getId().toString(), AuditResults.FAILURE, ipAddress);
            throw new AccountNotActiveException();
        }

        loginAttemptTracker.recordSuccess(email);
        user.recordLogin();

        String token = jwtService.issueToken(user.getId(), user.getEmail(), user.getRole().name());
        auditService.record(user.getId(), email, AuditActions.LOGIN, "User", user.getId().toString(), AuditResults.SUCCESS, ipAddress);

        return new AuthResponse(token, "Bearer", jwtService.expirationSeconds(), user.getId(), user.getRole());
    }

    public void logout(String userIdString, String email, String ipAddress) {
        // Stateless JWT: there is no server-side session to invalidate.
        // This records the audit event and returns success; the client
        // is responsible for discarding the token. A server-side
        // revocation/blocklist (needed for "log out everywhere" or
        // immediate revocation) is a P1 backlog item, not built here.
        java.util.UUID userId = userIdString == null ? null : java.util.UUID.fromString(userIdString);
        auditService.record(userId, email, AuditActions.LOGOUT, "User", userIdString, AuditResults.SUCCESS, ipAddress);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    // BCrypt hash of a random unused value — never a real user's hash.
    // Used only to keep failed-login timing consistent regardless of
    // whether the email exists.
    private static final String DUMMY_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOhi5L4pMv/i3nQ9YWkkaBpMdD7EW7iiG";
}
