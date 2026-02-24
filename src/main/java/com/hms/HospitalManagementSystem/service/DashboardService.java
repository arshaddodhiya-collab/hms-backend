package com.hms.HospitalManagementSystem.service;

import com.hms.HospitalManagementSystem.dto.response.ActivityDTO;
import com.hms.HospitalManagementSystem.dto.response.DashboardDTO;

import java.util.List;

public interface DashboardService {
    DashboardDTO getStats();

    List<ActivityDTO> getRecentActivity();
}
