package com.hms.HospitalManagementSystem.dto.response;

import com.hms.HospitalManagementSystem.enums.ServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeCatalogResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private ServiceType serviceType;
    private BigDecimal standardPrice;
    private boolean isActive;
}
