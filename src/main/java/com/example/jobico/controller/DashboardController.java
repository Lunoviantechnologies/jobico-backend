package com.example.jobico.controller;

import com.example.jobico.dto.DashboardStatsResponse;
import com.example.jobico.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provides live dashboard statistics for the React admin frontend.
 *
 * GET /api/admin/dashboard/stats
 *   → { totalCandidates, totalSelected, totalOnboarded, totalActiveEmployees,
 *       totalPayrollThisMonth, applied, shortlisted, selected, rejected }
 */
@RestController
@RequestMapping("/api/admin/dashboard")
public class DashboardController {

    @Autowired private DashboardService dashboardService;

    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}
