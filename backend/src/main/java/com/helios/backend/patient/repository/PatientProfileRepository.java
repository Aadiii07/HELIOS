package com.helios.backend.patient.repository;

import com.helios.backend.patient.domain.PatientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatientProfileRepository extends JpaRepository<PatientProfile, UUID> {

    Optional<PatientProfile> findByUserId(UUID userId);
}
