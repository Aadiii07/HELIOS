package com.helios.backend.documents.extraction;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extracts raw text from a document's bytes. {@link PdfTextExtractor}
 * is the only real implementation today (PDF only). Image formats
 * (JPG/PNG) are NOT extracted in this phase — that needs real OCR
 * (e.g. a Tesseract-backed implementation), which requires a native
 * dependency this project deliberately hasn't added yet (see
 * ADR-010). DocumentExtractionService marks images
 * UNSUPPORTED_FORMAT rather than faking a result.
 */
public interface TextExtractor {

    boolean supports(String contentType);

    String extractText(InputStream content) throws IOException;
}
