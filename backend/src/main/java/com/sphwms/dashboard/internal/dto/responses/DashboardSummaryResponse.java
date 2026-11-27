package com.sphwms.dashboard.internal.dto.responses;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalProducts,
        long arrivalsThisMonth,
        BigDecimal stockValue,
        BigDecimal totalDue
) {
}
