package com.helios.backend.observations.controller;

import com.helios.backend.common.web.ClientIpResolver;
import com.helios.backend.identity.security.AuthenticatedUser;
import com.helios.backend.observations.dto.ManualObservationRequest;
import com.helios.backend.observations.dto.ObservationResponse;
import com.helios.backend.observations.service.ObservationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/observations")
@PreAuthorize("hasRole('PATIENT')")
public class ObservationController {

    private final ObservationService service;

    public ObservationController(ObservationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ObservationResponse> list(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String code,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return service.list(user.id(), code, pageable);
    }

    @GetMapping("/{id}")
    public ObservationResponse getOne(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable UUID id) {
        return service.getOne(user.id(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ObservationResponse createManual(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody ManualObservationRequest request,
            HttpServletRequest servletRequest
    ) {
        return service.createManual(user.id(), request, ClientIpResolver.resolve(servletRequest));
    }
}
