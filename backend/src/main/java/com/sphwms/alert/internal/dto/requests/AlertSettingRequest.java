package com.sphwms.alert.internal.dto.requests;

public record AlertSettingRequest(
        Integer nearExpiryDays,
        Integer donationDays,
        Integer licenseExpiryDays,
        Integer dueReminderDays,
        Boolean lowStockEnabled,
        Boolean nearExpiryEnabled,
        Boolean dueEnabled,
        Boolean licenseEnabled
) {
}
