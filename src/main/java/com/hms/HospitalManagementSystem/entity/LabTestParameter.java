package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lab_test_parameters")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@lombok.experimental.SuperBuilder(toBuilder = true)
public class LabTestParameter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_test_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonBackReference
    private LabTestCatalog labTest;

    @Column(name = "parameter_name", nullable = false)
    private String parameterName;

    private String unit;

    @Column(name = "reference_range")
    private String referenceRange;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;

}
