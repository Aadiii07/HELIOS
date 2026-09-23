package com.helios.backend.documents.dto;

import com.helios.backend.documents.domain.ExtractionStatus;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        ExtractionStatus extractionStatus,
        Instant createdAt
) {
}
