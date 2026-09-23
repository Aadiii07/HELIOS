package com.helios.backend.documents.domain;

/**
 * Coarse, rule-derived confidence — not a model probability. HIGH
 * means the value's unit matched a known unit dictionary; MEDIUM
 * means the pattern matched but the unit was unrecognized or absent;
 * there is no LOW case emitted by the current extractor (reserved for
 * a future, noisier extraction method) — see LabValueCandidateExtractor.
 */
public enum Confidence {
    HIGH,
    MEDIUM,
    LOW
}
