package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Admission;
import com.hms.HospitalManagementSystem.enums.AdmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdmissionRepository extends JpaRepository<Admission, Long> {

    List<Admission> findByStatus(AdmissionStatus status);

    org.springframework.data.domain.Slice<Admission> findByStatus(AdmissionStatus status,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT a FROM Admission a WHERE a.patient.id = :patientId AND a.status = 'ADMITTED'")
    Optional<Admission> findActiveByPatientId(@Param("patientId") Long patientId);

    List<Admission> findByPatientId(Long patientId);
}
