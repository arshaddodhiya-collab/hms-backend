package com.hms.HospitalManagementSystem.dto.request;

import com.hms.HospitalManagementSystem.enums.ServiceType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChargeCatalogRequest {

    @NotBlank(message = "Code is required")
    private String code;

    @NotBlank(message = "Name is required")
    private String name;

    private String description;

    @NotNull(message = "Service type is required")
    private ServiceType serviceType;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", message = "Price cannot be negative")
    private BigDecimal standardPrice;

    private boolean isActive = true;
}
