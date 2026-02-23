package com.hms.HospitalManagementSystem.entity;

import com.hms.HospitalManagementSystem.enums.AppointmentStatus;
import com.hms.HospitalManagementSystem.enums.AppointmentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
        @Index(name = "idx_appointment_patient", columnList = "patient_id"),
        @Index(name = "idx_appointment_doctor", columnList = "doctor_id"),
        @Index(name = "idx_appointment_date", columnList = "start_date_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class Appointment extends BaseEntity implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private User doctor;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AppointmentType type;

    @Column(length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        if (this.status == null) {
            this.status = AppointmentStatus.SCHEDULED;
        }
    }

    // Stubbed helper methods for Encounter integration
    public boolean hasEncounter() {
        // TODO: Implement when Encounter entity is available
        return false;
    }

    public boolean hasActiveEncounter() {
        // TODO: Implement when Encounter entity is available
        // return encounter != null && encounter.getStatus() !=
        // EncounterStatus.COMPLETED;
        return false;
    }
}
