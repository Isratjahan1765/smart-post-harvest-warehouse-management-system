package com.sphwms.alert.internal.controller;

import com.sphwms.alert.internal.dto.requests.AlertSettingRequest;
import com.sphwms.alert.internal.dto.responses.AlertResponse;
import com.sphwms.alert.internal.dto.responses.AlertSummaryResponse;
import com.sphwms.alert.internal.dto.responses.AlertTypeSummaryResponse;
import com.sphwms.alert.internal.entity.AlertSetting;
import com.sphwms.alert.internal.service.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping("/summary")
    public ResponseEntity<AlertSummaryResponse> getSummary() {
        return ResponseEntity.ok(alertService.getSummary());
    }

    @GetMapping("/by-type")
    public ResponseEntity<List<AlertTypeSummaryResponse>> getByType() {
        return ResponseEntity.ok(alertService.getByTypeSummary());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(alertService.getUnreadCount());
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> getAll(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {
        return ResponseEntity.ok(alertService.getAllAlerts(type, status, q));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, Object>> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.markRead(id));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Object>> markAllRead() {
        return ResponseEntity.ok(alertService.markAllRead());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlertResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.getAlertById(id));
    }

    @GetMapping("/settings")
    public ResponseEntity<AlertSetting> getSettings() {
        return ResponseEntity.ok(alertService.getSettings());
    }

    @PutMapping("/settings")
    public ResponseEntity<AlertSetting> updateSettings(@RequestBody AlertSettingRequest req) {
        return ResponseEntity.ok(alertService.updateSettings(req));
    }
}
