package com.hms.HospitalManagementSystem.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BillingSummaryResponse {
    private Long patientId;
    private BigDecimal totalBilledAmount;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalDueAmount;
    private Integer outstandingInvoiceCount;
    private List<InvoiceResponse> recentInvoices;
}
