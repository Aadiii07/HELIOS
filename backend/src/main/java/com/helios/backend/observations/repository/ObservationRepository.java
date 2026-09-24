package com.helios.backend.observations.repository;

import com.helios.backend.observations.domain.Observation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    Page<Observation> findAllByPatientIdOrderByEffectiveDateDesc(UUID patientId, Pageable pageable);

    Page<Observation> findAllByPatientIdAndCodeOrderByEffectiveDateDesc(UUID patientId, String code, Pageable pageable);

    Optional<Observation> findByIdAndPatientId(UUID id, UUID patientId);
}
