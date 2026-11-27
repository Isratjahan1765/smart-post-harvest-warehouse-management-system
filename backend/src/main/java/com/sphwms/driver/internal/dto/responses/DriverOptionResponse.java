package com.sphwms.driver.internal.dto.responses;

public record DriverOptionResponse(
        Long driverId,
        String name,
        String vehicle,
        boolean active
) {
}
