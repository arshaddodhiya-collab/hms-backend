package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.LabRequest;
import com.hms.HospitalManagementSystem.enums.LabRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabRequestRepository extends JpaRepository<LabRequest, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "labTest", "labTest.parameters",
            "labResults" })
    List<LabRequest> findByEncounterId(Long encounterId);

    List<LabRequest> findByPatientId(Long patientId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "labTest", "labTest.parameters",
            "labResults" })
    List<LabRequest> findByStatusIn(List<LabRequestStatus> statuses);

    @Query("SELECT lr FROM LabRequest lr " +
            "LEFT JOIN FETCH lr.labResults " +
            "LEFT JOIN FETCH lr.labTest lt " +
            "LEFT JOIN FETCH lt.parameters " +
            "WHERE lr.id = :id")
    java.util.Optional<LabRequest> findByIdWithResults(@Param("id") Long id);

    long countByStatus(LabRequestStatus status);

    List<LabRequest> findTop5ByOrderByCreatedAtDesc();
}
