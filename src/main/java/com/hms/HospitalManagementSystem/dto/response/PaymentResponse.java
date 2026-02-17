package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PaymentResponse {
    private Long id;
    private Long invoiceId;
    private BigDecimal amount;
    private String paymentMethod;
    private String transactionReference;
    private String status;
    private LocalDateTime paymentDate;
    private Long receivedBy;
    private String notes;
}
