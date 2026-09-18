package com.helios.backend.common.audit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Persists an audit event as part of the CALLER's transaction
     * (default REQUIRED propagation) rather than a separate one.
     *
     * This was originally REQUIRES_NEW on the theory that audit writes
     * should be isolated from the business transaction. That's wrong
     * whenever the audited row references something created earlier in
     * the same still-open transaction (e.g. auditing a just-registered
     * user): a REQUIRES_NEW transaction can't see the other
     * transaction's uncommitted insert, so any foreign key to it fails
     * immediately. Joining the caller's transaction fixes that — and
     * as a deliberate trade-off, it also means a failure to write the
     * audit row now rolls back the action it was auditing. For a
     * system where "no audit trail" for a sensitive action (register,
     * login, ...) is itself unacceptable, failing closed like this is
     * the correct behavior, not a bug to work around.
     */
    @Transactional
    public void record(UUID actorUserId, String actorEmail, String action,
                        String resourceType, String resourceId, String result, String ipAddress) {
        repository.save(new AuditEvent(actorUserId, actorEmail, action, resourceType, resourceId, result, ipAddress));
    }
}
