package com.hms.HospitalManagementSystem.entity;

import com.hms.HospitalManagementSystem.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "invoice")
public class Invoice extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admission_id")
    private Admission admission;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal netAmount;

    @Builder.Default
    @Column(precision = 10, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal dueAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    private LocalDateTime issueDate;

    private LocalDateTime dueDate;

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceItem> items = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL)
    private List<Payment> payments = new ArrayList<>();
}
