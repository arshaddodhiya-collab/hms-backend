package com.hms.HospitalManagementSystem.dto.request;

import lombok.Data;

@Data
public class EncounterUpdateRequest {
    private String chiefComplaint;
    private String diagnosis;
    private String notes;
}
