package com.hms.HospitalManagementSystem.entity;

import com.hms.HospitalManagementSystem.enums.EncounterStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

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
@SuperBuilder(toBuilder = true)
public class Encounter extends BaseEntity {

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

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    // Relationships (Owned)
    // Vitals History (OneToMany)
    @Builder.Default
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vitals> vitalsHistory = new ArrayList<>();

    // Rounds (OneToMany)
    @Builder.Default
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Round> rounds = new ArrayList<>();

    /**
     * Helper to get the latest vitals.
     * Assumes vitals are added in chronological order or sorted.
     */
    public Vitals getVitals() {
        if (vitalsHistory == null || vitalsHistory.isEmpty()) {
            return null;
        }
        // Return last added (latest)
        return vitalsHistory.get(vitalsHistory.size() - 1);
    }

    @Builder.Default
    @OneToMany(mappedBy = "encounter", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Prescription> prescriptions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = EncounterStatus.TRIAGE;
        }
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }
}
