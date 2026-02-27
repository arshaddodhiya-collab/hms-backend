package com.hms.HospitalManagementSystem.dto.response;

import com.hms.HospitalManagementSystem.entity.Patient;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class PatientResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private Integer age;
    private Patient.Gender gender;
    private Patient.BloodGroup bloodGroup;
    private String contact;
    private String email;
    private String address;
    private String avatar;
    private LocalDate lastVisit;
    private String allergies;

    // Audit fields
    private String createdBy;
    private String lastModifiedBy;
    private java.time.LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;

    public String getName() {
        return firstName + " " + lastName;
    }
}
