package com.helios.backend.documents.validation;

import org.springframework.web.multipart.MultipartFile;

/**
 * Validates an uploaded file before it's accepted. {@link MagicByteFileContentValidator}
 * checks the declared content-type against an allow-list and verifies
 * the file's actual bytes match what it claims to be (rejecting a
 * renamed/spoofed upload). This is real, meaningful validation — but
 * it is NOT a virus/malware scan. A real scanner (e.g. a ClamAV
 * integration) is the production hardening step for master spec
 * §15's "malware/security validation hook" and is a clean P1 addition
 * behind this same interface — not built here.
 */
public interface FileContentValidator {
    void validate(MultipartFile file);
}
