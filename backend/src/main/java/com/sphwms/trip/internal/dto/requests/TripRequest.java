package com.sphwms.trip.internal.dto.requests;

import com.sphwms.trip.internal.enums.TripStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record TripRequest(
        @NotNull(message = "Driver is required.")
        Long driverId,

        @NotBlank(message = "Origin is required.")
        String origin,

        @NotBlank(message = "Destination is required.")
        String destination,

        @NotNull(message = "Trip date is required.")
        LocalDate tripDate,

        Long warehouseId,

        TripStatus status
) {
}
