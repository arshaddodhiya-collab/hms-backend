package com.hms.HospitalManagementSystem.dto.request;

import com.hms.HospitalManagementSystem.entity.Patient;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientUpdateRequest {
    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private Patient.Gender gender;
    private Patient.BloodGroup bloodGroup;
    private String contact;
    private String email;
    private String address;
    private String allergies;
    private String avatar;
    private Long version; // Optimistic locking
}
