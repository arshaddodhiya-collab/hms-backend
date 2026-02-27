package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "vitals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@lombok.experimental.SuperBuilder(toBuilder = true)
public class Vitals extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encounter_id", nullable = false)
    private Encounter encounter;

    // Vital Signs
    @Column(precision = 5, scale = 2)
    private BigDecimal temperature; // Celsius

    @Column
    private Integer systolic; // mmHg

    @Column
    private Integer diastolic; // mmHg

    @Column
    private Integer pulse; // bpm

    @Column
    private Integer spo2; // %

    @Column(precision = 5, scale = 2)
    private BigDecimal weight; // kg

    @Column(precision = 5, scale = 2)
    private BigDecimal height; // cm

    @Column(precision = 5, scale = 2)
    private BigDecimal bmi; // Calculated

    // Audit
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by", nullable = false)
    private User recordedBy; // Nurse who recorded vitals

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }
}
