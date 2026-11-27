package com.sphwms.transportcost.internal.dto.responses;

import java.math.BigDecimal;

public record MonthlyTripCostResponse(
        String month,
        BigDecimal total
) {
}
