package com.hms.HospitalManagementSystem.controller;

import com.hms.HospitalManagementSystem.dto.response.ActivityDTO;
import com.hms.HospitalManagementSystem.dto.response.DashboardDTO;
import com.hms.HospitalManagementSystem.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardDTO> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/activity")
    public ResponseEntity<List<ActivityDTO>> getRecentActivity() {
        return ResponseEntity.ok(dashboardService.getRecentActivity());
    }
}
