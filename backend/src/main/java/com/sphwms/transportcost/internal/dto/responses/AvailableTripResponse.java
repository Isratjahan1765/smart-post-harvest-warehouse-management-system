package com.sphwms.transportcost.internal.dto.responses;

import java.time.LocalDate;

public record AvailableTripResponse(
        Long tripId,
        String tripNo,
        String driverName,
        String origin,
        String destination,
        LocalDate tripDate
) {
}
