package com.helios.backend.identity.controller;

import com.helios.backend.common.web.ClientIpResolver;
import com.helios.backend.identity.dto.AuthResponse;
import com.helios.backend.identity.dto.ChangePasswordRequest;
import com.helios.backend.identity.dto.LoginRequest;
import com.helios.backend.identity.dto.RegisterRequest;
import com.helios.backend.identity.dto.UserSummaryResponse;
import com.helios.backend.identity.security.AuthenticatedUser;
import com.helios.backend.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        return authService.register(request, ClientIpResolver.resolve(servletRequest));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        return authService.login(request, ClientIpResolver.resolve(servletRequest));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthenticatedUser user, HttpServletRequest servletRequest) {
        authService.logout(user.id().toString(), user.email(), ClientIpResolver.resolve(servletRequest));
    }

    @GetMapping("/me")
    public AuthenticatedUser me(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }

    @PostMapping("/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest servletRequest
    ) {
        authService.changePassword(user.id(), request, ClientIpResolver.resolve(servletRequest));
    }
}
