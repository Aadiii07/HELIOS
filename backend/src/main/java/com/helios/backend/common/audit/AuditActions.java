package com.helios.backend.common.audit;

/**
 * Audit action names (master spec §27). Extended as later phases add
 * their own sensitive actions (DOCUMENT_UPLOAD, CONSENT_CREATED, ...).
 */
public final class AuditActions {

    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String PROFILE_CREATED = "PROFILE_CREATED";
    public static final String PROFILE_UPDATED = "PROFILE_UPDATED";
    public static final String DOCUMENT_UPLOAD = "DOCUMENT_UPLOAD";
    public static final String DOCUMENT_VIEW = "DOCUMENT_VIEW";
    public static final String DOCUMENT_DELETE = "DOCUMENT_DELETE";
    public static final String CANDIDATE_REVIEWED = "CANDIDATE_REVIEWED";

    private AuditActions() {
    }
}
