package com.helios.backend.observations.util;

import java.util.regex.Pattern;

/**
 * Turns a free-text display name (e.g. "Hemoglobin A1c") into a
 * normalized code (e.g. "HEMOGLOBIN_A1C"). This is a simple
 * deterministic transform — NOT a mapping to a real clinical coding
 * system (LOINC, SNOMED, etc.). It exists so the same measurement
 * typed/extracted with different capitalization or spacing groups
 * together, not to provide clinical-grade interoperability. A real
 * coding system mapping is a distinct, much larger effort for a
 * later phase (see master spec §45, FHIR/Interoperability).
 */
public final class CodeNormalizer {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Z0-9]+");

    private CodeNormalizer() {
    }

    public static String toCode(String displayName) {
        String upper = displayName.trim().toUpperCase();
        String code = NON_ALPHANUMERIC.matcher(upper).replaceAll("_");
        code = code.replaceAll("^_+|_+$", "");
        return code.isEmpty() ? "UNKNOWN" : code;
    }
}
