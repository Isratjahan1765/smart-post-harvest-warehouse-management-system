package com.sphwms.driver.internal.dto.requests;

import com.sphwms.driver.internal.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record DriverRequest(
        @NotBlank(message = "Driver name is required.")
        String name,

        @NotBlank(message = "License number is required.")
        String licenseNo,

        LocalDate licenseExpiry,

        @NotBlank(message = "Phone number is required.")
        String phone,

        @NotNull(message = "Vehicle type is required.")
        VehicleType vehicleType,

        @NotBlank(message = "Vehicle number is required.")
        String vehicleNo
) {
}
