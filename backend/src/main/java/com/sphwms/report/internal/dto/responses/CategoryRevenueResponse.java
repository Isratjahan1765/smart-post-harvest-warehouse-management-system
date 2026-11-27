package com.sphwms.report.internal.dto.responses;

import java.math.BigDecimal;

public record CategoryRevenueResponse(
        String category,
        BigDecimal revenue,
        int percent
) {
}
