package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.ChargeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChargeCatalogRepository extends JpaRepository<ChargeCatalog, Long> {
    Optional<ChargeCatalog> findByCode(String code);

    org.springframework.data.domain.Slice<ChargeCatalog> findAllBy(org.springframework.data.domain.Pageable pageable);
}
