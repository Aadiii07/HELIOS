package com.helios.backend.documents.service;

import com.helios.backend.common.audit.AuditActions;
import com.helios.backend.common.audit.AuditResults;
import com.helios.backend.common.audit.AuditService;
import com.helios.backend.documents.domain.Document;
import com.helios.backend.documents.domain.ExtractionCandidate;
import com.helios.backend.documents.domain.ExtractionStatus;
import com.helios.backend.documents.domain.ReviewStatus;
import com.helios.backend.documents.dto.CandidateResponse;
import com.helios.backend.documents.dto.CandidateReviewRequest;
import com.helios.backend.documents.exception.CandidateAlreadyReviewedException;
import com.helios.backend.documents.exception.CandidateNotFoundException;
import com.helios.backend.documents.exception.DocumentNotFoundException;
import com.helios.backend.documents.exception.InvalidCandidateReviewException;
import com.helios.backend.documents.extraction.ExtractedField;
import com.helios.backend.documents.extraction.LabValueCandidateExtractor;
import com.helios.backend.documents.extraction.TextExtractor;
import com.helios.backend.documents.repository.DocumentRepository;
import com.helios.backend.documents.repository.ExtractionCandidateRepository;
import com.helios.backend.observations.service.ObservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentExtractionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractionService.class);

    private final DocumentRepository documentRepository;
    private final ExtractionCandidateRepository candidateRepository;
    private final List<TextExtractor> textExtractors;
    private final LabValueCandidateExtractor candidateExtractor;
    private final AuditService auditService;
    private final ObservationService observationService;

    public DocumentExtractionService(
            DocumentRepository documentRepository,
            ExtractionCandidateRepository candidateRepository,
            List<TextExtractor> textExtractors,
            LabValueCandidateExtractor candidateExtractor,
            AuditService auditService,
            ObservationService observationService
    ) {
        this.documentRepository = documentRepository;
        this.candidateRepository = candidateRepository;
        this.textExtractors = textExtractors;
        this.candidateExtractor = candidateExtractor;
        this.auditService = auditService;
        this.observationService = observationService;
    }

    /**
     * Runs synchronously as part of the upload request. PDF text
     * extraction of a typical lab-report-sized document is fast
     * enough that this doesn't need background-job infrastructure —
     * see ADR-010. If extraction genuinely becomes expensive (e.g.
     * once OCR is added), that's the point to introduce async
     * processing, not before.
     */
    @Transactional
    public void processDocument(Document document, MultipartFile file) {
        TextExtractor extractor = textExtractors.stream()
                .filter(e -> e.supports(document.getContentType()))
                .findFirst()
                .orElse(null);

        if (extractor == null) {
            document.markExtractionStatus(ExtractionStatus.UNSUPPORTED_FORMAT);
            documentRepository.save(document);
            return;
        }

        try (InputStream content = file.getInputStream()) {
            String text = extractor.extractText(content);
            List<ExtractedField> fields = candidateExtractor.extract(text);

            for (ExtractedField field : fields) {
                ExtractionCandidate candidate = new ExtractionCandidate(
                        document.getId(), document.getPatientId(),
                        field.fieldLabel(), field.rawValue(), field.unit(), field.referenceRange(),
                        field.sourceExcerpt(), field.confidence(), LabValueCandidateExtractor.METHOD_NAME
                );
                candidateRepository.save(candidate);
            }

            document.markExtractionStatus(ExtractionStatus.PROCESSED);
        } catch (IOException e) {
            log.warn("Extraction failed for document {}", document.getId(), e);
            document.markExtractionStatus(ExtractionStatus.FAILED);
        }

        documentRepository.save(document);
    }

    @Transactional(readOnly = true)
    public List<CandidateResponse> listCandidates(UUID patientId, UUID documentId) {
        requireOwnedDocument(patientId, documentId);
        return candidateRepository.findAllByDocumentIdAndPatientIdOrderByCreatedAt(documentId, patientId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CandidateResponse review(UUID patientId, UUID documentId, UUID candidateId,
                                     CandidateReviewRequest request, String ipAddress) {
        Document document = requireOwnedDocument(patientId, documentId);

        ExtractionCandidate candidate = candidateRepository.findByIdAndPatientId(candidateId, patientId)
                .orElseThrow(CandidateNotFoundException::new);
        if (!candidate.getDocumentId().equals(documentId)) {
            throw new CandidateNotFoundException();
        }
        if (candidate.getReviewStatus() != ReviewStatus.PENDING) {
            // Without this guard, reviewing the same candidate twice
            // (e.g. CONFIRM then later REJECT) would create a second,
            // duplicate Observation below.
            throw new CandidateAlreadyReviewedException();
        }

        LocalDate effectiveDate = request.effectiveDate() != null
                ? request.effectiveDate()
                : document.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();

        switch (request.reviewStatus()) {
            case CONFIRMED -> {
                candidate.confirm();
                observationService.createFromCandidate(
                        patientId, candidate.getFieldLabel(), candidate.getRawValue(), candidate.getUnit(),
                        candidate.getReferenceRange(), effectiveDate, documentId, candidate.getId(), ipAddress
                );
            }
            case REJECTED -> candidate.reject();
            case CORRECTED -> {
                if (isBlank(request.correctedFieldLabel()) || isBlank(request.correctedRawValue())) {
                    throw new InvalidCandidateReviewException(
                            "correctedFieldLabel and correctedRawValue are required when reviewStatus is CORRECTED");
                }
                candidate.correct(request.correctedFieldLabel(), request.correctedRawValue(), request.correctedUnit());
                observationService.createFromCandidate(
                        patientId, request.correctedFieldLabel(), request.correctedRawValue(), request.correctedUnit(),
                        candidate.getReferenceRange(), effectiveDate, documentId, candidate.getId(), ipAddress
                );
            }
            case PENDING -> throw new InvalidCandidateReviewException(
                    "reviewStatus must be CONFIRMED, REJECTED, or CORRECTED");
        }

        candidate = candidateRepository.save(candidate);

        auditService.record(patientId, null, AuditActions.CANDIDATE_REVIEWED,
                "ExtractionCandidate", candidate.getId().toString(), AuditResults.SUCCESS, ipAddress);

        return toResponse(candidate);
    }

    private Document requireOwnedDocument(UUID patientId, UUID documentId) {
        return documentRepository.findByIdAndPatientIdAndDeletedAtIsNull(documentId, patientId)
                .orElseThrow(DocumentNotFoundException::new);
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private CandidateResponse toResponse(ExtractionCandidate c) {
        return new CandidateResponse(
                c.getId(), c.getDocumentId(), c.getFieldLabel(), c.getRawValue(), c.getUnit(),
                c.getReferenceRange(), c.getSourceExcerpt(), c.getConfidence(), c.getExtractionMethod(),
                c.getReviewStatus(), c.getCorrectedFieldLabel(), c.getCorrectedRawValue(),
                c.getCorrectedUnit(), c.getReviewedAt(), c.getCreatedAt()
        );
    }
}
