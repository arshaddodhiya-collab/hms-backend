package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    private Prescription prescription;

    @Column(name = "medicine_name", nullable = false, length = 255)
    private String medicineName;

    @Column(nullable = false, length = 100)
    private String dosage; // e.g., "500mg"

    @Column(nullable = false, length = 100)
    private String frequency; // e.g., "1-0-1"

    @Column(nullable = false, length = 100)
    private String duration; // e.g., "5 days"
}
