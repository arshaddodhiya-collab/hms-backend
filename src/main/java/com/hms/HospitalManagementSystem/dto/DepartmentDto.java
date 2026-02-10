package com.hms.HospitalManagementSystem.dto;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentDto {
    private Long id;
    private String name;
    private String description;
    private String headOfDepartmentName; // Display name of HOD
    private Long headOfDepartmentId; // ID for updates
    private int staffCount; // Derived field
    private boolean active;
}
