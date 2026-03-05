package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Department;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    Optional<Department> findByName(String name);

    boolean existsByName(String name);

    Slice<Department> findAllBy(org.springframework.data.domain.Pageable pageable);
}
