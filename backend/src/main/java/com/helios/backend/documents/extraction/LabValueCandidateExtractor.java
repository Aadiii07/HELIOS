package com.helios.backend.documents.extraction;

import com.helios.backend.documents.domain.Confidence;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Best-effort, deterministic (no ML/LLM) parser for lines that look
 * like "<test name> <value> <unit> <reference range>" — the common
 * shape of a row in a lab report. This is NOT a general lab-report
 * parser: real reports vary enormously in layout, and PDFTextStripper's
 * column spacing is itself imperfect. Every result is a candidate
 * with PENDING review status — master spec §16 is explicit that
 * extracted data is never auto-trusted, and this extractor's accuracy
 * is exactly why that rule exists.
 *
 * Parses right-to-left by whitespace-separated tokens (any run of 1+
 * whitespace — NOT dependent on multi-space column gaps, which vary
 * a lot between PDF generators): trailing reference range (1-3
 * tokens, e.g. "4.0-5.6", "> 40", "0 - 100"), then a unit if the
 * token before it is numeric, then the numeric value itself, with
 * everything remaining as the label. This handles labels that are
 * themselves multiple words (e.g. "Fasting Glucose").
 */
@Component
public class LabValueCandidateExtractor {

    public static final String METHOD_NAME = "PDF_TEXT_PATTERN_MATCH_V1";

    private static final Pattern WHITESPACE_SPLIT = Pattern.compile("\\s+");

    private static final Pattern NUMERIC_VALUE = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    private static final Pattern REFERENCE_RANGE = Pattern.compile(
            "^(?:[<>≤≥]\\s?-?\\d+(\\.\\d+)?|-?\\d+(\\.\\d+)?\\s*(?:-|to)\\s*-?\\d+(\\.\\d+)?)$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> KNOWN_UNITS = Set.of(
            "%", "mg/dl", "mmol/l", "g/dl", "miu/l", "iu/l", "ng/ml", "pg/ml",
            "mmhg", "bpm", "kg", "lb", "cm", "in", "/ul", "meq/l", "u/l", "mcg/dl"
    );

    public List<ExtractedField> extract(String text) {
        List<ExtractedField> results = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return results;
        }

        for (String line : text.split("\\r?\\n")) {
            parseLine(line).ifPresent(results::add);
        }
        return results;
    }

    private Optional<ExtractedField> parseLine(String line) {
        String trimmed = line.strip();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }

        String[] tokens = WHITESPACE_SPLIT.split(trimmed);
        if (tokens.length < 2) {
            return Optional.empty();
        }

        int end = tokens.length; // exclusive — tokens[0, end) are still unconsumed

        String referenceRange = null;
        for (int span = Math.min(3, end - 1); span >= 1; span--) {
            String joined = String.join(" ", Arrays.copyOfRange(tokens, end - span, end));
            if (REFERENCE_RANGE.matcher(joined).matches()) {
                referenceRange = joined;
                end -= span;
                break;
            }
        }

        if (end < 2) {
            return Optional.empty();
        }

        String unit = null;
        String valueToken;

        if (NUMERIC_VALUE.matcher(tokens[end - 1]).matches()) {
            valueToken = tokens[end - 1];
            end -= 1;
        } else if (NUMERIC_VALUE.matcher(tokens[end - 2]).matches()) {
            unit = tokens[end - 1];
            valueToken = tokens[end - 2];
            end -= 2;
        } else {
            return Optional.empty();
        }

        if (end < 1) {
            return Optional.empty();
        }

        String label = String.join(" ", Arrays.copyOfRange(tokens, 0, end));
        if (label.length() < 2 || !containsLetter(label)) {
            return Optional.empty();
        }

        Confidence confidence = Confidence.MEDIUM;
        if (unit != null && KNOWN_UNITS.contains(unit.toLowerCase())) {
            confidence = Confidence.HIGH;
        }

        return Optional.of(new ExtractedField(label, valueToken, unit, referenceRange, trimmed, confidence));
    }

    private boolean containsLetter(String s) {
        return s.chars().anyMatch(Character::isLetter);
    }
}
