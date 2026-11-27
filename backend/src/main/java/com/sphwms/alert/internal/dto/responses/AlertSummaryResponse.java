package com.sphwms.alert.internal.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AlertSummaryResponse(
        long lowStock,
        long nearExpiry,
        long duePayment,
        long licenseExpiry,
        long total,
        long unread,

        @JsonProperty("LOW_STOCK")
        long lowStockUpper,

        @JsonProperty("NEAR_EXPIRY")
        long nearExpiryUpper,

        @JsonProperty("DUE_PAYMENT")
        long duePaymentUpper,

        @JsonProperty("LICENSE_EXPIRY")
        long licenseExpiryUpper
) {
    public AlertSummaryResponse(long lowStock, long nearExpiry, long duePayment, long licenseExpiry, long unread) {
        this(lowStock, nearExpiry, duePayment, licenseExpiry, lowStock + nearExpiry + duePayment + licenseExpiry, unread,
             lowStock, nearExpiry, duePayment, licenseExpiry);
    }
}
