package com.helios.backend.documents.validation;

import com.helios.backend.documents.exception.InvalidFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

@Service
public class MagicByteFileContentValidator implements FileContentValidator {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png"
    );

    // First bytes each format actually starts with, regardless of what
    // the client claims. Catches a renamed .exe uploaded as "report.pdf",
    // for example — the Content-Type header alone is client-controlled
    // and proves nothing on its own.
    private static final Map<String, byte[]> SIGNATURES = Map.of(
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46}, // %PDF
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47}
    );

    private final long maxSizeBytes;

    public MagicByteFileContentValidator(@Value("${document.upload.max-size-bytes:20971520}") long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    @Override
    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("No file was uploaded");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new InvalidFileException("File exceeds the maximum allowed size of " + maxSizeBytes + " bytes");
        }

        String declaredType = file.getContentType();
        if (declaredType == null || !ALLOWED_CONTENT_TYPES.contains(declaredType)) {
            throw new InvalidFileException("Unsupported file type — only PDF, JPG, and PNG are accepted");
        }

        byte[] signature = SIGNATURES.get(declaredType);
        byte[] header = readHeader(file, signature.length);
        for (int i = 0; i < signature.length; i++) {
            if (header[i] != signature[i]) {
                throw new InvalidFileException(
                        "File content does not match its declared type (" + declaredType + ")");
            }
        }
    }

    private byte[] readHeader(MultipartFile file, int length) {
        byte[] header = new byte[length];
        try (InputStream in = file.getInputStream()) {
            int read = in.readNBytes(header, 0, length);
            if (read < length) {
                throw new InvalidFileException("File is too small to be a valid document");
            }
        } catch (IOException e) {
            throw new InvalidFileException("Could not read uploaded file");
        }
        return header;
    }
}
