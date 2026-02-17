package com.hms.HospitalManagementSystem.entity;

import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "encounters", indexes = {
        @Index(name = "idx_encounter_appointment", columnList = "appointment_id"),
        @Index(name = "idx_encounter_patient", columnList = "patient_id"),
        @Index(name = "idx_encounter_doctor", columnList = "doctor_id"),
        @Index(name = "idx_encounter_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Encounter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Relationships
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", unique = true)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id")
    private Admission admission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    // Clinical Data
    @Column(name = "chief_complaint", columnDefinition = "TEXT")
    private String chiefComplaint;

    @Column(columnDefinition = "TEXT")
    private String diagnosis;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Status & Lifecycle
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EncounterStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    // Audit Fields
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // Relationships (Owned)
    @Builder.Default
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vitals> vitalsList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prescription> prescriptions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Round> rounds = new ArrayList<>();

    // Removed LabRequest for now as per analysis it says "TO BE IMPLEMENTED" (wait,
    // labRequests is in analysis code block, but not implemented in step 1 list.
    // Analysis says LabRequest -> LabResult. But in Step 3.1 code block it shows
    // LabRequest list.
    // The user prompts "implement encounter module analysis". The analysis includes
    // LabRequest in the code block.
    // However, I don't want to create LabRequest entity if it's not requested or if
    // it already exists/doesn't exist.
    // Step 1 list in task.md does NOT mention LabRequest entity.
    // I will comment it out or omit it for now to avoid errors if LabRequest
    // doesn't exist. Checking existing files... LabRequest is NOT in the file list
    // I saw earlier (Patient, Appointment, User, etc). I saw Department,
    // MedicalHistory... no LabRequest.
    // So I will omit LabRequest for now.

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = EncounterStatus.TRIAGE;
        }
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
