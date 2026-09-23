package com.helios.backend.documents.extraction;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class PdfTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String contentType) {
        return "application/pdf".equals(contentType);
    }

    @Override
    public String extractText(InputStream content) throws IOException {
        // Documents are already size-capped by upload validation
        // (MagicByteFileContentValidator), so reading fully into memory
        // here is bounded and acceptable — PDFBox's Loader needs
        // random access to the bytes, not a forward-only stream.
        byte[] bytes = content.readAllBytes();
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
