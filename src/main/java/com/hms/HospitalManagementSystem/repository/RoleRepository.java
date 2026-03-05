package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Role;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);

    Slice<Role> findAllBy(org.springframework.data.domain.Pageable pageable);
}
