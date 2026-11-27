package com.sphwms.donation.internal.dto.responses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EligibleDonationResponse(
        Long batchId,
        String batchNo,
        Long productId,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal remaining,
        BigDecimal remainingQuantity,
        LocalDate expiryDate,
        long daysUntilExpiry
) {
}
