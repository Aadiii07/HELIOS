package com.helios.backend.documents.repository;

import com.helios.backend.documents.domain.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    // Every query here is scoped by patientId — there is no
    // findById-only method exposed to the service layer, so it is
    // structurally impossible to fetch a document without also
    // checking ownership (master spec §13 IDOR prevention).

    Optional<Document> findByIdAndPatientIdAndDeletedAtIsNull(UUID id, UUID patientId);

    Page<Document> findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID patientId, Pageable pageable);
}
