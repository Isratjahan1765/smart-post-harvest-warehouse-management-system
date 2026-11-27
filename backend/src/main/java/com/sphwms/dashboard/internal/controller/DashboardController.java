package com.sphwms.dashboard.internal.controller;

import com.sphwms.dashboard.internal.dto.responses.DashboardSummaryResponse;
import com.sphwms.dashboard.internal.dto.responses.InventoryRankingItem;
import com.sphwms.dashboard.internal.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/inventory-ranking")
    public ResponseEntity<List<InventoryRankingItem>> getInventoryRanking(@RequestParam(defaultValue = "15") int limit) {
        return ResponseEntity.ok(dashboardService.getInventoryRanking(limit));
    }
}
