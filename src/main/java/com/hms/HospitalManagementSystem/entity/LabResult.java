package com.hms.HospitalManagementSystem.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "lab_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@lombok.experimental.SuperBuilder(toBuilder = true)
public class LabResult extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lab_request_id", nullable = false)
    private LabRequest labRequest;

    @Column(name = "parameter_name", nullable = false)
    private String parameterName;

    @Column(name = "result_value", nullable = false)
    private String resultValue;

    @Column(length = 50)
    private String unit;

    @Column(name = "reference_range")
    private String referenceRange;

    @Column(name = "is_abnormal")
    private Boolean abnormal;

}
