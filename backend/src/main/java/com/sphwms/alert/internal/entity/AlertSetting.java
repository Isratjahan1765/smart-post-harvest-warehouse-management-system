package com.sphwms.alert.internal.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_setting")
@Getter
@Setter
public class AlertSetting {

    @Id
    @Column(name = "setting_id")
    private Integer settingId = 1;

    @Column(name = "near_expiry_days", nullable = false)
    private Integer nearExpiryDays = 7;

    @Column(name = "donation_days", nullable = false)
    private Integer donationDays = 2;

    @Column(name = "license_expiry_days", nullable = false)
    private Integer licenseExpiryDays = 30;

    @Column(name = "due_reminder_days", nullable = false)
    private Integer dueReminderDays = 15;

    @Column(name = "low_stock_enabled", nullable = false)
    private Boolean lowStockEnabled = true;

    @Column(name = "near_expiry_enabled", nullable = false)
    private Boolean nearExpiryEnabled = true;

    @Column(name = "due_enabled", nullable = false)
    private Boolean dueEnabled = true;

    @Column(name = "license_enabled", nullable = false)
    private Boolean licenseEnabled = true;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
