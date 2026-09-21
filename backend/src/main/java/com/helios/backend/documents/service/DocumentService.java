package com.helios.backend.documents.service;

import com.helios.backend.common.audit.AuditActions;
import com.helios.backend.common.audit.AuditResults;
import com.helios.backend.common.audit.AuditService;
import com.helios.backend.documents.domain.Document;
import com.helios.backend.documents.dto.DocumentResponse;
import com.helios.backend.documents.exception.DocumentNotFoundException;
import com.helios.backend.documents.exception.InvalidFileException;
import com.helios.backend.documents.repository.DocumentRepository;
import com.helios.backend.documents.storage.ObjectStorageService;
import com.helios.backend.documents.validation.FileContentValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class DocumentService {

    private final DocumentRepository repository;
    private final ObjectStorageService storage;
    private final FileContentValidator validator;
    private final AuditService auditService;

    public DocumentService(
            DocumentRepository repository,
            ObjectStorageService storage,
            FileContentValidator validator,
            AuditService auditService
    ) {
        this.repository = repository;
        this.storage = storage;
        this.validator = validator;
        this.auditService = auditService;
    }

    @Transactional
    public DocumentResponse upload(UUID patientId, MultipartFile file, String ipAddress) {
        validator.validate(file);

        String checksum;
        String storageKey;
        try (InputStream raw = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // Single pass: bytes flow client -> DigestInputStream (hashing
            // as they go) -> storage. Never buffered into a byte[] in memory.
            try (DigestInputStream digestStream = new DigestInputStream(raw, digest)) {
                storageKey = storage.store(digestStream, file.getSize(), file.getContentType());
            }
            checksum = HexFormat.of().formatHex(digest.digest());
        } catch (IOException e) {
            throw new InvalidFileException("Could not read uploaded file");
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a JDK-guaranteed algorithm; this is unreachable
            // in practice, but the checked exception must be handled.
            throw new IllegalStateException("SHA-256 unavailable", e);
        }

        Document document = new Document(
                patientId, file.getOriginalFilename(), storageKey,
                file.getContentType(), file.getSize(), checksum
        );
        document = repository.save(document);

        auditService.record(patientId, null, AuditActions.DOCUMENT_UPLOAD,
                "Document", document.getId().toString(), AuditResults.SUCCESS, ipAddress);

        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public Page<DocumentResponse> list(UUID patientId, Pageable pageable) {
        return repository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public DocumentResponse getMetadata(UUID patientId, UUID documentId) {
        return toResponse(findOwned(patientId, documentId));
    }

    @Transactional
    public DownloadableDocument downloadContent(UUID patientId, UUID documentId, String ipAddress) {
        Document document = findOwned(patientId, documentId);
        InputStream content = storage.retrieve(document.getStorageKey());

        auditService.record(patientId, null, AuditActions.DOCUMENT_VIEW,
                "Document", document.getId().toString(), AuditResults.SUCCESS, ipAddress);

        return new DownloadableDocument(document.getOriginalFilename(), document.getContentType(), content);
    }

    @Transactional
    public void delete(UUID patientId, UUID documentId, String ipAddress) {
        Document document = findOwned(patientId, documentId);
        storage.delete(document.getStorageKey());
        document.markDeleted();
        repository.save(document);

        auditService.record(patientId, null, AuditActions.DOCUMENT_DELETE,
                "Document", document.getId().toString(), AuditResults.SUCCESS, ipAddress);
    }

    private Document findOwned(UUID patientId, UUID documentId) {
        return repository.findByIdAndPatientIdAndDeletedAtIsNull(documentId, patientId)
                .orElseThrow(DocumentNotFoundException::new);
    }

    private DocumentResponse toResponse(Document d) {
        return new DocumentResponse(
                d.getId(), d.getOriginalFilename(), d.getContentType(),
                d.getSizeBytes(), d.getChecksumSha256(), d.getCreatedAt()
        );
    }

    public record DownloadableDocument(String filename, String contentType, InputStream content) {
    }
}
