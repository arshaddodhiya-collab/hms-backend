package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Vitals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VitalsRepository extends JpaRepository<Vitals, Long> {

    Optional<Vitals> findByEncounterId(Long encounterId);

    Optional<Vitals> findFirstByEncounterPatientIdOrderByRecordedAtDesc(Long patientId);
}
