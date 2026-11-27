package com.sphwms.report.internal.dto.responses;

import java.math.BigDecimal;

public record ReportSummaryResponse(
        BigDecimal totalRevenue,
        BigDecimal revenue,
        BigDecimal totalExpense,
        BigDecimal expense,
        BigDecimal netProfit,
        BigDecimal donatedValue
) {
    public ReportSummaryResponse(BigDecimal revenue, BigDecimal expense, BigDecimal netProfit, BigDecimal donatedValue) {
        this(revenue, revenue, expense, expense, netProfit, donatedValue);
    }
}
