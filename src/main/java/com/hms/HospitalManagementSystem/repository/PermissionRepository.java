package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);

    org.springframework.data.domain.Slice<Permission> findAllBy(org.springframework.data.domain.Pageable pageable);
}
