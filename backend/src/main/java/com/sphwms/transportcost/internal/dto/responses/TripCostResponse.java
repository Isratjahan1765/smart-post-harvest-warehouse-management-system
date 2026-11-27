package com.sphwms.transportcost.internal.dto.responses;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record TripCostResponse(
        Long tripCostId,
        Long tripId,
        String tripNo,
        String driverName,
        String origin,
        String destination,
        BigDecimal fuelCost,
        BigDecimal driverCost,
        BigDecimal otherCost,
        BigDecimal totalCost,
        String note,
        LocalDate tripDate,
        LocalDateTime createdAt
) {
}
