package com.helios.backend.timeline.service;

import com.helios.backend.documents.domain.Document;
import com.helios.backend.documents.repository.DocumentRepository;
import com.helios.backend.observations.domain.Observation;
import com.helios.backend.observations.domain.ObservationSource;
import com.helios.backend.observations.repository.ObservationRepository;
import com.helios.backend.observations.util.CodeNormalizer;
import com.helios.backend.timeline.dto.TimelineEventResponse;
import com.helios.backend.timeline.dto.TimelineEventType;
import com.helios.backend.timeline.dto.TimelinePageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class TimelineService {

    private final DocumentRepository documentRepository;
    private final ObservationRepository observationRepository;

    public TimelineService(DocumentRepository documentRepository, ObservationRepository observationRepository) {
        this.documentRepository = documentRepository;
        this.observationRepository = observationRepository;
    }

    /**
     * Merges documents and observations in application code rather
     * than a single SQL query — the simplest correct approach for two
     * distinct tables at MVP scale (master spec §50, no
     * overengineering ahead of an actual need). Uses Pageable.unpaged()
     * against each repository, so this reads a patient's ENTIRE
     * document/observation history into memory before filtering and
     * paging the merged result. Fine for the data volumes an early
     * product sees; a patient with thousands of documents/observations
     * would need a real DB-level UNION or cursor-based approach —
     * documented here as the point to revisit, not a current problem.
     */
    @Transactional(readOnly = true)
    public TimelinePageResponse getTimeline(
            UUID patientId, LocalDate from, LocalDate to, String code,
            ObservationSource source, UUID documentId, int page, int size
    ) {
        String normalizedCode = (code == null || code.isBlank()) ? null : CodeNormalizer.toCode(code);

        List<Document> documents = documentRepository
                .findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId, Pageable.unpaged())
                .getContent();
        List<Observation> observations = observationRepository
                .findAllByPatientIdOrderByEffectiveDateDesc(patientId, Pageable.unpaged())
                .getContent();

        List<TimelineEventResponse> events = new ArrayList<>();

        for (Document d : documents) {
            // code/source filters are observation-specific; a document
            // event never matches them, so it's excluded when either is set.
            if (normalizedCode != null || source != null) continue;
            if (documentId != null && !documentId.equals(d.getId())) continue;

            LocalDate eventDate = d.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (from != null && eventDate.isBefore(from)) continue;
            if (to != null && eventDate.isAfter(to)) continue;

            events.add(new TimelineEventResponse(
                    TimelineEventType.DOCUMENT_UPLOADED, eventDate,
                    "Document uploaded: " + d.getOriginalFilename(),
                    d.getId(), null, null, null, null, d.getCreatedAt()
            ));
        }

        for (Observation o : observations) {
            if (normalizedCode != null && !normalizedCode.equals(o.getCode())) continue;
            if (source != null && o.getSource() != source) continue;
            if (documentId != null && !documentId.equals(o.getSourceDocumentId())) continue;

            LocalDate eventDate = o.getEffectiveDate();
            if (from != null && eventDate.isBefore(from)) continue;
            if (to != null && eventDate.isAfter(to)) continue;

            String title = o.getDisplayName() + ": " + o.getRawValue() + (o.getUnit() != null ? " " + o.getUnit() : "");
            events.add(new TimelineEventResponse(
                    TimelineEventType.OBSERVATION_RECORDED, eventDate, title,
                    o.getSourceDocumentId(), o.getId(), o.getCode(), o.getSource(), o.getConfidence(), o.getCreatedAt()
            ));
        }

        events.sort(
                Comparator.comparing(TimelineEventResponse::eventDate)
                        .thenComparing(TimelineEventResponse::createdAt)
                        .reversed()
        );

        int totalElements = events.size();
        int totalPages = size > 0 ? (int) Math.ceil(totalElements / (double) size) : 0;
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);
        List<TimelineEventResponse> pageContent = events.subList(fromIndex, toIndex);

        return new TimelinePageResponse(pageContent, totalElements, totalPages, page, size);
    }
}
