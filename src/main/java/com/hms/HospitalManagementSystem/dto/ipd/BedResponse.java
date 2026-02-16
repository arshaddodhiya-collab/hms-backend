package com.hms.HospitalManagementSystem.dto.ipd;

import com.hms.HospitalManagementSystem.dto.ipd.WardResponse;
import com.hms.HospitalManagementSystem.enums.BedType;
import lombok.Data;

@Data
public class BedResponse {
    private Long id;
    private String number;
    private BedType type;
    private boolean isOccupied;
    private boolean isActive;
    private WardResponse ward;
}
