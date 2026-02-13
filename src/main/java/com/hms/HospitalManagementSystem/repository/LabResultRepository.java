package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.LabResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabResultRepository extends JpaRepository<LabResult, Long> {
    List<LabResult> findByLabRequestId(Long labRequestId);
}
