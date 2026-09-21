package com.helios.backend.patient.service;

import com.helios.backend.common.audit.AuditActions;
import com.helios.backend.common.audit.AuditResults;
import com.helios.backend.common.audit.AuditService;
import com.helios.backend.patient.domain.PatientProfile;
import com.helios.backend.patient.dto.PatientProfileRequest;
import com.helios.backend.patient.dto.PatientProfileResponse;
import com.helios.backend.patient.exception.ProfileNotFoundException;
import com.helios.backend.patient.repository.PatientProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PatientProfileService {

    private final PatientProfileRepository repository;
    private final AuditService auditService;

    public PatientProfileService(PatientProfileRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    // Every method here takes the authenticated user's own ID, resolved
    // server-side from the validated JWT (see PatientProfileController) —
    // never a client-supplied profile/user ID. There is structurally no
    // "fetch by arbitrary ID" path, which is what prevents IDOR here
    // rather than a per-request ownership check (master spec §13).

    @Transactional(readOnly = true)
    public PatientProfileResponse getOwnProfile(UUID userId) {
        PatientProfile profile = repository.findByUserId(userId)
                .orElseThrow(ProfileNotFoundException::new);
        return toResponse(profile);
    }

    @Transactional
    public PatientProfileResponse upsertOwnProfile(UUID userId, PatientProfileRequest request, String ipAddress) {
        var existing = repository.findByUserId(userId);
        boolean isNew = existing.isEmpty();
        PatientProfile profile = existing.orElseGet(() -> new PatientProfile(userId));

        profile.applyUpdate(
                request.firstName(), request.lastName(), request.dateOfBirth(), request.phoneNumber(),
                request.addressLine1(), request.addressLine2(), request.city(), request.state(),
                request.postalCode(), request.country(), request.preferredLanguage()
        );
        profile = repository.save(profile);

        auditService.record(
                userId, null,
                isNew ? AuditActions.PROFILE_CREATED : AuditActions.PROFILE_UPDATED,
                "PatientProfile", profile.getId().toString(), AuditResults.SUCCESS, ipAddress
        );

        return toResponse(profile);
    }

    private PatientProfileResponse toResponse(PatientProfile p) {
        return new PatientProfileResponse(
                p.getId(), p.getUserId(), p.getFirstName(), p.getLastName(), p.getDateOfBirth(),
                p.getPhoneNumber(), p.getAddressLine1(), p.getAddressLine2(), p.getCity(), p.getState(),
                p.getPostalCode(), p.getCountry(), p.getPreferredLanguage(), p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}
