package com.sphwms.trip.internal.dto.responses;

import com.sphwms.trip.internal.enums.TripStatus;
import java.time.LocalDate;

public record TripResponse(
        Long tripId,
        String tripNo,
        Long driverId,
        String driverName,
        String origin,
        String destination,
        LocalDate tripDate,
        TripStatus status,
        Long warehouseId,
        String warehouseName
) {
}
