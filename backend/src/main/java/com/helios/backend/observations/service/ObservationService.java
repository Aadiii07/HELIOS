package com.helios.backend.observations.service;

import com.helios.backend.common.audit.AuditActions;
import com.helios.backend.common.audit.AuditResults;
import com.helios.backend.common.audit.AuditService;
import com.helios.backend.documents.domain.Confidence;
import com.helios.backend.observations.domain.Observation;
import com.helios.backend.observations.domain.ObservationSource;
import com.helios.backend.observations.dto.ManualObservationRequest;
import com.helios.backend.observations.dto.ObservationResponse;
import com.helios.backend.observations.exception.ObservationNotFoundException;
import com.helios.backend.observations.repository.ObservationRepository;
import com.helios.backend.observations.util.CodeNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class ObservationService {

    private final ObservationRepository repository;
    private final AuditService auditService;

    public ObservationService(ObservationRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    /**
     * Called only from a patient's CONFIRMED/CORRECTED review of an
     * extraction candidate (see DocumentExtractionService) — never
     * automatically from extraction alone. Confidence is always HIGH
     * here: a human has just verified this value, which matters more
     * than whatever confidence the extractor originally assigned.
     */
    @Transactional
    public Observation createFromCandidate(
            UUID patientId, String displayName, String rawValue, String unit, String referenceRange,
            LocalDate effectiveDate, UUID sourceDocumentId, UUID sourceCandidateId, String ipAddress
    ) {
        Observation observation = new Observation(
                patientId, CodeNormalizer.toCode(displayName), displayName, rawValue, parseNumeric(rawValue),
                unit, referenceRange, effectiveDate, ObservationSource.DOCUMENT_EXTRACTION,
                sourceDocumentId, sourceCandidateId, Confidence.HIGH
        );
        observation = repository.save(observation);

        auditService.record(patientId, null, AuditActions.OBSERVATION_CREATE,
                "Observation", observation.getId().toString(), AuditResults.SUCCESS, ipAddress);

        return observation;
    }

    @Transactional
    public ObservationResponse createManual(UUID patientId, ManualObservationRequest request, String ipAddress) {
        Observation observation = new Observation(
                patientId, CodeNormalizer.toCode(request.displayName()), request.displayName(),
                request.value(), parseNumeric(request.value()), request.unit(), request.referenceRange(),
                request.effectiveDate(), ObservationSource.MANUAL_ENTRY, null, null, Confidence.HIGH
        );
        observation = repository.save(observation);

        auditService.record(patientId, null, AuditActions.OBSERVATION_CREATE,
                "Observation", observation.getId().toString(), AuditResults.SUCCESS, ipAddress);

        return toResponse(observation);
    }

    @Transactional(readOnly = true)
    public Page<ObservationResponse> list(UUID patientId, String code, Pageable pageable) {
        Page<Observation> page = (code == null || code.isBlank())
                ? repository.findAllByPatientIdOrderByEffectiveDateDesc(patientId, pageable)
                : repository.findAllByPatientIdAndCodeOrderByEffectiveDateDesc(patientId, CodeNormalizer.toCode(code), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ObservationResponse getOne(UUID patientId, UUID id) {
        Observation observation = repository.findByIdAndPatientId(id, patientId)
                .orElseThrow(ObservationNotFoundException::new);
        return toResponse(observation);
    }

    private Double parseNumeric(String rawValue) {
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException e) {
            // Not every observation value is numeric (e.g. a future
            // qualitative result like "Positive"/"Negative") — the
            // flexible model (master spec §17) allows that; numeric
            // analysis simply skips observations where this is null.
            return null;
        }
    }

    public ObservationResponse toResponse(Observation o) {
        return new ObservationResponse(
                o.getId(), o.getCode(), o.getDisplayName(), o.getRawValue(), o.getNumericValue(),
                o.getUnit(), o.getReferenceRange(), o.getEffectiveDate(), o.getSource(),
                o.getSourceDocumentId(), o.getSourceCandidateId(), o.getConfidence(), o.getStatus(), o.getCreatedAt()
        );
    }
}
