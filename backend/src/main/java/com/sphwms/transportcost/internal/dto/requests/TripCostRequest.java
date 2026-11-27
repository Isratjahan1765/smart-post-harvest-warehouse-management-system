package com.sphwms.transportcost.internal.dto.requests;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TripCostRequest(
        @NotNull(message = "Trip ID is required.")
        Long tripId,

        BigDecimal fuelCost,
        BigDecimal driverCost,
        BigDecimal otherCost,
        String note
) {
}
