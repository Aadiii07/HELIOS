package com.helios.backend.documents.repository;

import com.helios.backend.documents.domain.ExtractionCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExtractionCandidateRepository extends JpaRepository<ExtractionCandidate, UUID> {

    // Scoped by patientId, same IDOR-prevention pattern as DocumentRepository.

    List<ExtractionCandidate> findAllByDocumentIdAndPatientIdOrderByCreatedAt(UUID documentId, UUID patientId);

    Optional<ExtractionCandidate> findByIdAndPatientId(UUID id, UUID patientId);
}
