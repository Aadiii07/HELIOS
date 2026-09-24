package com.helios.backend.observations.domain;

import com.helios.backend.documents.domain.Confidence;
import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "observations")
public class Observation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "raw_value", nullable = false)
    private String rawValue;

    @Column(name = "numeric_value")
    private Double numericValue;

    @Column
    private String unit;

    @Column(name = "reference_range")
    private String referenceRange;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObservationSource source;

    @Column(name = "source_document_id")
    private UUID sourceDocumentId;

    @Column(name = "source_candidate_id")
    private UUID sourceCandidateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Confidence confidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ObservationStatus status = ObservationStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Observation() {
        // JPA
    }

    public Observation(
            UUID patientId, String code, String displayName, String rawValue, Double numericValue,
            String unit, String referenceRange, LocalDate effectiveDate, ObservationSource source,
            UUID sourceDocumentId, UUID sourceCandidateId, Confidence confidence
    ) {
        this.patientId = patientId;
        this.code = code;
        this.displayName = displayName;
        this.rawValue = rawValue;
        this.numericValue = numericValue;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.effectiveDate = effectiveDate;
        this.source = source;
        this.sourceDocumentId = sourceDocumentId;
        this.sourceCandidateId = sourceCandidateId;
        this.confidence = confidence;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRawValue() {
        return rawValue;
    }

    public Double getNumericValue() {
        return numericValue;
    }

    public String getUnit() {
        return unit;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public LocalDate getEffectiveDate() {
        return effectiveDate;
    }

    public ObservationSource getSource() {
        return source;
    }

    public UUID getSourceDocumentId() {
        return sourceDocumentId;
    }

    public UUID getSourceCandidateId() {
        return sourceCandidateId;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public ObservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
