package com.helios.backend.documents.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String checksumSha256,
        Instant createdAt
) {
}
