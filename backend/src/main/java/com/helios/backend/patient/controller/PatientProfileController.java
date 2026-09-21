package com.helios.backend.patient.controller;

import com.helios.backend.common.web.ClientIpResolver;
import com.helios.backend.identity.security.AuthenticatedUser;
import com.helios.backend.patient.dto.PatientProfileRequest;
import com.helios.backend.patient.dto.PatientProfileResponse;
import com.helios.backend.patient.service.PatientProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patient/profile")
@PreAuthorize("hasRole('PATIENT')")
public class PatientProfileController {

    private final PatientProfileService service;

    public PatientProfileController(PatientProfileService service) {
        this.service = service;
    }

    @GetMapping
    public PatientProfileResponse getOwnProfile(@AuthenticationPrincipal AuthenticatedUser user) {
        return service.getOwnProfile(user.id());
    }

    @PutMapping
    public PatientProfileResponse upsertOwnProfile(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PatientProfileRequest request,
            HttpServletRequest servletRequest
    ) {
        return service.upsertOwnProfile(user.id(), request, ClientIpResolver.resolve(servletRequest));
    }
}
