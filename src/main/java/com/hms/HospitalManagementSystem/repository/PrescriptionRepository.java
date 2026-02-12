package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Prescription;
import com.hms.HospitalManagementSystem.enums.PrescriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    List<Prescription> findByEncounterId(Long encounterId);

    Optional<Prescription> findByEncounterIdAndStatus(Long encounterId, PrescriptionStatus status);
}
