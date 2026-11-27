package com.sphwms.warehouse.internal.dto.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record WarehouseRequest(
        @NotBlank(message = "Warehouse name is required.")
        String name,

        @NotBlank(message = "Location is required.")
        String location,

        String inCharge,

        @NotNull(message = "Capacity must be greater than zero.")
        @Positive(message = "Capacity must be greater than zero.")
        BigDecimal capacityKg,

        String status
) {
}
