package com.hms.HospitalManagementSystem.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EncounterUpdateRequest {
    @Size(max = 500, message = "Chief complaint must not exceed 500 characters")
    private String chiefComplaint;

    @Size(max = 1000, message = "Diagnosis must not exceed 1000 characters")
    private String diagnosis;

    @Size(max = 2000, message = "Notes must not exceed 2000 characters")
    private String notes;
}
