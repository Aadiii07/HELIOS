package com.helios.backend.documents.exception;

import com.helios.backend.common.error.ApiException;
import org.springframework.http.HttpStatus;

/**
 * Thrown both when a document truly doesn't exist AND when it exists
 * but belongs to a different patient — deliberately the same
 * response either way, so a request for someone else's document ID
 * can't be used to probe which IDs exist (master spec §13, §24).
 */
public class DocumentNotFoundException extends ApiException {
    public DocumentNotFoundException() {
        super(HttpStatus.NOT_FOUND, "DOCUMENT_NOT_FOUND", "No document found with that ID");
    }
}
