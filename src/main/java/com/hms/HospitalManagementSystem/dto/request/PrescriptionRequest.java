package com.hms.HospitalManagementSystem.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PrescriptionRequest {
    private String note;
    private List<PrescriptionItemRequest> items;
}
