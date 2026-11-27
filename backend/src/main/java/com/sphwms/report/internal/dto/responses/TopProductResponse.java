package com.sphwms.report.internal.dto.responses;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String productName,
        BigDecimal revenue,
        BigDecimal quantity,
        BigDecimal cost,
        BigDecimal profit
) {
}
