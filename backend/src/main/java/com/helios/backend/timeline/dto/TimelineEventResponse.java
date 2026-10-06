package com.helios.backend.timeline.dto;

import com.helios.backend.documents.domain.Confidence;
import com.helios.backend.observations.domain.ObservationSource;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One entry on the timeline. Every event traces back to exactly one
 * underlying record — documentId for DOCUMENT_UPLOADED,
 * observationId (plus, when applicable, the document/candidate it
 * came from — see Observation) for OBSERVATION_RECORDED — never a
 * synthesized or unverifiable fact (master spec §18).
 */
public record TimelineEventResponse(
        TimelineEventType eventType,
        LocalDate eventDate,
        String title,
        UUID documentId,
        UUID observationId,
        String code,
        ObservationSource source,
        Confidence confidence,
        Instant createdAt
) {
}
