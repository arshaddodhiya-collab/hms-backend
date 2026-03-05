package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndId(String username, Long id);

    List<User> findByRoles_Name(String roleName);

    org.springframework.data.domain.Slice<User> findByRoles_Name(String roleName,
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u WHERE NOT EXISTS (SELECT 1 FROM u.roles r WHERE r.name = 'PATIENT')")
    org.springframework.data.domain.Slice<User> findNonPatientUsers(org.springframework.data.domain.Pageable pageable);
}
