package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.LabTestCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LabTestCatalogRepository extends JpaRepository<LabTestCatalog, Long> {
    Optional<LabTestCatalog> findByCode(String code);
}
