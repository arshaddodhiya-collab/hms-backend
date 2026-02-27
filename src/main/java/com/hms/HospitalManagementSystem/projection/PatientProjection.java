package com.hms.HospitalManagementSystem.projection;

import com.hms.HospitalManagementSystem.entity.Patient;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDate;

public interface PatientProjection {
    Long getId();

    String getFirstName();

    String getLastName();

    LocalDate getDob();

    @Value("#{target.firstName + ' ' + target.lastName}")
    String getName();

    @Value("#{target.dob != null ? T(java.time.Period).between(target.dob, T(java.time.LocalDate).now()).getYears() : null}")
    Integer getAge();

    Patient.Gender getGender();

    Patient.BloodGroup getBloodGroup();

    String getContact();

    String getEmail();

    String getAddress();

    String getAvatar();

    String getAllergies();

    // Audit fields
    String getCreatedBy();

    String getLastModifiedBy();

    java.time.LocalDateTime getCreatedAt();

    java.time.LocalDateTime getUpdatedAt();
}
