package com.hms.HospitalManagementSystem.enums;

public enum EncounterStatus {
    TRIAGE, // Nurse is recording vitals
    IN_PROGRESS, // Doctor is consulting
    COMPLETED, // Consultation finished
    CANCELLED // Encounter cancelled
}
