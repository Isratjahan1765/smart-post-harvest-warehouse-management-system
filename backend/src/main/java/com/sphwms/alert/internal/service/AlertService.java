package com.sphwms.alert.internal.service;

import com.sphwms.alert.internal.dto.requests.AlertSettingRequest;
import com.sphwms.alert.internal.dto.responses.AlertResponse;
import com.sphwms.alert.internal.dto.responses.AlertSummaryResponse;
import com.sphwms.alert.internal.dto.responses.AlertTypeSummaryResponse;
import com.sphwms.alert.internal.entity.Alert;
import com.sphwms.alert.internal.entity.AlertSetting;
import com.sphwms.alert.internal.enums.AlertType;
import com.sphwms.alert.internal.repository.AlertRepository;
import com.sphwms.alert.internal.repository.AlertSettingRepository;
import com.sphwms.shared.audit.AuditService;
import com.sphwms.shared.exception.NotFoundException;
import com.sphwms.shared.security.CurrentUserProvider;
import com.sphwms.warehouse.internal.entity.Warehouse;
import com.sphwms.warehouse.internal.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertSettingRepository alertSettingRepository;
    private final WarehouseRepository warehouseRepository;
    private final AlertRefreshService alertRefreshService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public AlertSummaryResponse getSummary() {
        long lowStock = alertRepository.countActiveByType(AlertType.LOW_STOCK);
        long nearExpiry = alertRepository.countActiveByType(AlertType.NEAR_EXPIRY);
        long duePayment = alertRepository.countActiveByType(AlertType.DUE_PAYMENT);
        long licenseExpiry = alertRepository.countActiveByType(AlertType.LICENSE_EXPIRY);
        long unread = alertRepository.countUnreadActiveAlerts();

        return new AlertSummaryResponse(lowStock, nearExpiry, duePayment, licenseExpiry, unread);
    }

    @Transactional(readOnly = true)
    public List<AlertTypeSummaryResponse> getByTypeSummary() {
        long lowStock = alertRepository.countActiveByType(AlertType.LOW_STOCK);
        long nearExpiry = alertRepository.countActiveByType(AlertType.NEAR_EXPIRY);
        long duePayment = alertRepository.countActiveByType(AlertType.DUE_PAYMENT);
        long licenseExpiry = alertRepository.countActiveByType(AlertType.LICENSE_EXPIRY);

        long total = lowStock + nearExpiry + duePayment + licenseExpiry;

        int lsPct = total > 0 ? (int) Math.round((double) lowStock / total * 100) : 0;
        int nePct = total > 0 ? (int) Math.round((double) nearExpiry / total * 100) : 0;
        int dpPct = total > 0 ? (int) Math.round((double) duePayment / total * 100) : 0;
        int lePct = total > 0 ? (int) Math.round((double) licenseExpiry / total * 100) : 0;

        return List.of(
                new AlertTypeSummaryResponse(AlertType.LOW_STOCK, "Low Stock", lowStock, lsPct),
                new AlertTypeSummaryResponse(AlertType.NEAR_EXPIRY, "Near Expiry", nearExpiry, nePct),
                new AlertTypeSummaryResponse(AlertType.DUE_PAYMENT, "Due Payment", duePayment, dpPct),
                new AlertTypeSummaryResponse(AlertType.LICENSE_EXPIRY, "License Expiry", licenseExpiry, lePct)
        );
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getUnreadCount() {
        return Map.of("count", alertRepository.countUnreadActiveAlerts());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts(String type, String status, String q) {
        List<Alert> alerts = alertRepository.findByResolvedAtIsNullOrderByIsReadAscAlertIdDesc();
        Map<Long, String> whNames = warehouseRepository.findAll().stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName, (a, b) -> a));

        String query = q != null ? q.trim().toLowerCase() : "";
        String typeFilter = (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type))
                ? type.trim().toUpperCase() : null;
        String statusFilter = (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status))
                ? status.trim().toUpperCase() : null;

        return alerts.stream()
                .filter(a -> {
                    if (typeFilter == null) return true;
                    return a.getType().name().equalsIgnoreCase(typeFilter);
                })
                .filter(a -> {
                    if (statusFilter == null) return true;
                    if ("UNREAD".equalsIgnoreCase(statusFilter)) return Boolean.FALSE.equals(a.getIsRead());
                    if ("READ".equalsIgnoreCase(statusFilter)) return Boolean.TRUE.equals(a.getIsRead());
                    return true;
                })
                .filter(a -> {
                    if (query.isEmpty()) return true;
                    return a.getItemName().toLowerCase().contains(query) ||
                           a.getDetails().toLowerCase().contains(query);
                })
                .map(a -> toResponse(a, whNames))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AlertResponse getAlertById(Long id) {
        Alert a = alertRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alert not found."));
        String whName = a.getWarehouseId() != null
                ? warehouseRepository.findById(a.getWarehouseId()).map(Warehouse::getName).orElse("")
                : null;
        return toResponse(a, a.getWarehouseId() != null ? Map.of(a.getWarehouseId(), whName) : Collections.emptyMap());
    }

    @Transactional
    public Map<String, Object> markRead(Long id) {
        Alert a = alertRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Alert not found."));

        if (!Boolean.TRUE.equals(a.getIsRead())) {
            a.setIsRead(true);
            a.setReadAt(LocalDateTime.now());
            a.setReadBy(currentUserProvider.getCurrentUserId());
            alertRepository.save(a);
        }

        long unread = alertRepository.countUnreadActiveAlerts();
        return Map.of("alertId", id, "isRead", true, "unreadCount", unread);
    }

    @Transactional
    public Map<String, Object> markAllRead() {
        List<Alert> alerts = alertRepository.findByResolvedAtIsNullOrderByIsReadAscAlertIdDesc();
        int count = 0;
        for (Alert a : alerts) {
            if (!Boolean.TRUE.equals(a.getIsRead())) {
                a.setIsRead(true);
                a.setReadAt(LocalDateTime.now());
                a.setReadBy(currentUserProvider.getCurrentUserId());
                alertRepository.save(a);
                count++;
            }
        }
        return Map.of("updated", count, "message", "All alerts marked as read.");
    }

    @Transactional(readOnly = true)
    public AlertSetting getSettings() {
        return alertSettingRepository.findById(1).orElseGet(() -> {
            AlertSetting s = new AlertSetting();
            s.setSettingId(1);
            return alertSettingRepository.save(s);
        });
    }

    @Transactional
    public AlertSetting updateSettings(AlertSettingRequest req) {
        AlertSetting s = getSettings();
        if (req.nearExpiryDays() != null) s.setNearExpiryDays(req.nearExpiryDays());
        if (req.donationDays() != null) s.setDonationDays(req.donationDays());
        if (req.licenseExpiryDays() != null) s.setLicenseExpiryDays(req.licenseExpiryDays());
        if (req.dueReminderDays() != null) s.setDueReminderDays(req.dueReminderDays());
        if (req.lowStockEnabled() != null) s.setLowStockEnabled(req.lowStockEnabled());
        if (req.nearExpiryEnabled() != null) s.setNearExpiryEnabled(req.nearExpiryEnabled());
        if (req.dueEnabled() != null) s.setDueEnabled(req.dueEnabled());
        if (req.licenseEnabled() != null) s.setLicenseEnabled(req.licenseEnabled());
        s.setUpdatedBy(currentUserProvider.getCurrentUserId());
        s.setUpdatedAt(LocalDateTime.now());

        AlertSetting saved = alertSettingRepository.save(s);
        auditService.log("ALERT_SETTINGS_UPDATE", "ALERT_SETTING", 1L, "Updated alert settings");

        // Trigger refresh
        alertRefreshService.refreshAllAlerts();

        return saved;
    }

    private AlertResponse toResponse(Alert a, Map<Long, String> whNames) {
        String whName = a.getWarehouseId() != null ? whNames.get(a.getWarehouseId()) : null;
        return new AlertResponse(
                a.getAlertId(),
                a.getType(),
                a.getEntityType(),
                a.getEntityId(),
                a.getWarehouseId(),
                whName,
                a.getItemName(),
                a.getDetails(),
                a.getDedupeKey(),
                Boolean.TRUE.equals(a.getIsRead()),
                a.getReadAt(),
                a.getCreatedAt(),
                a.getResolvedAt()
        );
    }
}
