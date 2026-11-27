package com.sphwms.driver.internal.dto.responses;

import com.sphwms.driver.internal.enums.VehicleType;
import java.time.LocalDate;

public record DriverResponse(
        Long driverId,
        Long userId,
        String name,
        String licenseNo,
        LocalDate licenseExpiry,
        String phone,
        VehicleType vehicleType,
        String vehicleNo,
        Boolean active
) {
}
