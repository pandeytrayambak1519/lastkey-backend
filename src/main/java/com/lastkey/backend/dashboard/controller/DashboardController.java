package com.lastkey.backend.dashboard.controller;

import com.lastkey.backend.dashboard.dto.DashboardResponse;
import com.lastkey.backend.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService
    ) {
        this.dashboardService =
                dashboardService;
    }

    @GetMapping
    public ResponseEntity<DashboardResponse>
    getCurrentUserDashboard() {

        DashboardResponse response =
                dashboardService
                        .getCurrentUserDashboard();

        return ResponseEntity.ok(response);
    }
}