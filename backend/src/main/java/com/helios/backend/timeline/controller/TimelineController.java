package com.helios.backend.timeline.controller;

import com.helios.backend.identity.security.AuthenticatedUser;
import com.helios.backend.observations.domain.ObservationSource;
import com.helios.backend.timeline.dto.TimelinePageResponse;
import com.helios.backend.timeline.service.TimelineService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timeline")
@PreAuthorize("hasRole('PATIENT')")
public class TimelineController {

    private final TimelineService service;

    public TimelineController(TimelineService service) {
        this.service = service;
    }

    @GetMapping
    public TimelinePageResponse getTimeline(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) ObservationSource source,
            @RequestParam(required = false) UUID documentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.getTimeline(user.id(), from, to, code, source, documentId, page, size);
    }
}
