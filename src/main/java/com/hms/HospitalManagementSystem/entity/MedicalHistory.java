package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "medical_histories")
@Getter
@Setter
public class MedicalHistory extends PublicEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private String conditionName;

    @Column(name = "diagnosed_date")
    private LocalDate diagnosedDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        ONGOING, HEALED, CHRONIC
    }
}
