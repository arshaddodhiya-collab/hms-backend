package com.hms.HospitalManagementSystem.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class DashboardDTO {
    private long totalPatients;
    private long expectedAppointmentsToday;
    private long pendingLabRequests;
    private BigDecimal todaysRevenue;
    private long criticalPatientsCount;
}
