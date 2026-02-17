package com.hms.HospitalManagementSystem.entity;

import com.hms.HospitalManagementSystem.enums.ChargeSource;
import com.hms.HospitalManagementSystem.enums.ChargeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "charge")
public class Charge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChargeSource sourceModule;

    @Column(nullable = false)
    private Long sourceReferenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "charge_catalog_id")
    private ChargeCatalog chargeCatalog;

    @Column(nullable = false, length = 100)
    private String itemName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(nullable = false)
    private Integer quantity = 1;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private ChargeStatus status = ChargeStatus.UNBILLED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Version
    private Long version;
}
