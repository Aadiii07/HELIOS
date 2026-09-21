package com.helios.backend.documents.storage;

import java.io.InputStream;

/**
 * Storage abstraction for document bytes. {@link LocalFilesystemStorageService}
 * is the real implementation used for native/local development and
 * testing (per master spec §10, Docker is never required to run the
 * app). An S3-compatible implementation is the intended production
 * target (see backend/.env.example's OBJECT_STORAGE_* variables) but
 * is not built yet — adding one means implementing this interface,
 * nothing else in the module should need to change.
 */
public interface ObjectStorageService {

    /**
     * Stores the given bytes under a server-generated key and returns
     * that key. Callers never choose the key themselves.
     */
    String store(InputStream content, long sizeBytes, String contentType);

    InputStream retrieve(String storageKey);

    void delete(String storageKey);
}
