package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByUsernameAndId(String username, Long id);

    List<User> findByRoles_Name(String roleName);
}
