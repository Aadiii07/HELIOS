package com.helios.backend.documents.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores document bytes on the local filesystem under a configurable
 * root directory. This is a REAL implementation (not a stub/mock) —
 * suitable for native single-instance development and demo use per
 * master spec §10 (Docker/external services are never required to
 * run the app). It is not suitable for a multi-instance production
 * deployment; see {@link ObjectStorageService}'s class doc.
 */
@Service
public class LocalFilesystemStorageService implements ObjectStorageService {

    private final Path root;

    public LocalFilesystemStorageService(@Value("${document.storage.root:./local-data/documents}") String rootPath) {
        this.root = Path.of(rootPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create document storage directory: " + root, e);
        }
    }

    @Override
    public String store(InputStream content, long sizeBytes, String contentType) {
        // Server-generated key only — never derived from client input,
        // so there is no path-traversal surface here at all.
        String key = UUID.randomUUID().toString();
        Path target = resolve(key);
        try {
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store document", e);
        }
        return key;
    }

    @Override
    public InputStream retrieve(String storageKey) {
        try {
            return Files.newInputStream(resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read document " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete document " + storageKey, e);
        }
    }

    private Path resolve(String storageKey) {
        // storageKey is always a UUID we generated ourselves (never
        // client input), but resolve+normalize+containment-check
        // defensively anyway rather than trusting that invariant alone.
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return resolved;
    }
}
