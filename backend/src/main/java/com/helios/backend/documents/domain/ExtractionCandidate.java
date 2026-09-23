package com.helios.backend.documents.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "extraction_candidates")
public class ExtractionCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "field_label", nullable = false)
    private String fieldLabel;

    @Column(name = "raw_value", nullable = false)
    private String rawValue;

    @Column
    private String unit;

    @Column(name = "reference_range")
    private String referenceRange;

    @Column(name = "source_excerpt", nullable = false, columnDefinition = "TEXT")
    private String sourceExcerpt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Confidence confidence;

    @Column(name = "extraction_method", nullable = false, length = 64)
    private String extractionMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 16)
    private ReviewStatus reviewStatus = ReviewStatus.PENDING;

    @Column(name = "corrected_field_label")
    private String correctedFieldLabel;

    @Column(name = "corrected_raw_value")
    private String correctedRawValue;

    @Column(name = "corrected_unit")
    private String correctedUnit;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ExtractionCandidate() {
        // JPA
    }

    public ExtractionCandidate(
            UUID documentId, UUID patientId, String fieldLabel, String rawValue,
            String unit, String referenceRange, String sourceExcerpt,
            Confidence confidence, String extractionMethod
    ) {
        this.documentId = documentId;
        this.patientId = patientId;
        this.fieldLabel = fieldLabel;
        this.rawValue = rawValue;
        this.unit = unit;
        this.referenceRange = referenceRange;
        this.sourceExcerpt = sourceExcerpt;
        this.confidence = confidence;
        this.extractionMethod = extractionMethod;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        this.reviewStatus = ReviewStatus.CONFIRMED;
        this.reviewedAt = Instant.now();
    }

    public void reject() {
        this.reviewStatus = ReviewStatus.REJECTED;
        this.reviewedAt = Instant.now();
    }

    public void correct(String fieldLabel, String rawValue, String unit) {
        // The original extracted values (above) are left untouched —
        // only these separate "corrected*" fields are set, so both the
        // machine's original guess and the patient's fix stay visible.
        this.reviewStatus = ReviewStatus.CORRECTED;
        this.correctedFieldLabel = fieldLabel;
        this.correctedRawValue = rawValue;
        this.correctedUnit = unit;
        this.reviewedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public String getFieldLabel() {
        return fieldLabel;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getUnit() {
        return unit;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public String getSourceExcerpt() {
        return sourceExcerpt;
    }

    public Confidence getConfidence() {
        return confidence;
    }

    public String getExtractionMethod() {
        return extractionMethod;
    }

    public ReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public String getCorrectedFieldLabel() {
        return correctedFieldLabel;
    }

    public String getCorrectedRawValue() {
        return correctedRawValue;
    }

    public String getCorrectedUnit() {
        return correctedUnit;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
