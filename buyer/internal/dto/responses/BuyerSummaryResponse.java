package com.sphwms.buyer.internal.dto.responses;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BuyerSummaryResponse(
        BigDecimal totalPurchase,
        BigDecimal totalPaid,
        BigDecimal totalDue,
        LocalDate lastPurchase,
        int paidPercentage
) {
}
