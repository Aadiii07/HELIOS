package com.helios.backend.documents.extraction;

import com.helios.backend.documents.domain.Confidence;

public record ExtractedField(
        String fieldLabel,
        String rawValue,
        String unit,
        String referenceRange,
        String sourceExcerpt,
        Confidence confidence
) {
}
