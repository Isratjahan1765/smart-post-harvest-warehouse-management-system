package com.sphwms.arrival.internal.dto.requests;

import com.sphwms.arrival.internal.enums.ArrivalGrade;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ArrivalRequest(
        @NotNull(message = "Required fields are missing.")
        Long productId,

        @NotNull(message = "Required fields are missing.")
        Long warehouseId,

        @NotNull(message = "Invalid quantity - enter a positive number.")
        @Positive(message = "Invalid quantity - enter a positive number.")
        BigDecimal quantity,

        @NotNull(message = "Required fields are missing.")
        ArrivalGrade grade,

        @NotNull(message = "Required fields are missing.")
        @Future(message = "Expiry date must be in the future.")
        LocalDate expiryDate,

        BigDecimal purchasePrice
) {
}
