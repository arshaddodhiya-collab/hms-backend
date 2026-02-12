package com.hms.HospitalManagementSystem.repository;

import com.hms.HospitalManagementSystem.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {

    Optional<Patient> findByContact(String contact);

    Optional<Patient> findByEmail(String email);

    @Query("""
    SELECT p FROM Patient p
    WHERE LOWER(TRIM(p.firstName)) = LOWER(TRIM(:firstName))
      AND LOWER(TRIM(p.lastName)) = LOWER(TRIM(:lastName))
      AND p.dob = :dob
      AND p.contact = :contact
""")
    Optional<Patient> findPotentialDuplicate(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("dob") LocalDate dob,
            @Param("contact") String contact);

    boolean existsByContact(String contact);

    boolean existsByEmail(String email);
}
