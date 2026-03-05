package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Ward;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WardRepository extends JpaRepository<Ward, Long> {
    Optional<Ward> findByName(String name);

    Slice<Ward> findAllBy(org.springframework.data.domain.Pageable pageable);
}
