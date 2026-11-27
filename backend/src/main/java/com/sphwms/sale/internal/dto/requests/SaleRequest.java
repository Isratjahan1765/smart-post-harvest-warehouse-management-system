package com.sphwms.sale.internal.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaleRequest(
        @NotNull(message = "Required fields are missing.")
        Long buyerId,

        @NotNull(message = "Required fields are missing.")
        Long productId,

        @NotNull(message = "Required fields are missing.")
        Long warehouseId,

        @NotNull(message = "Invalid quantity - enter a positive number.")
        @Positive(message = "Invalid quantity - enter a positive number.")
        BigDecimal quantity,

        BigDecimal rate,
        BigDecimal paidAmount,
        BigDecimal paidNow,
        LocalDate saleDate,
        LocalDate dueDate
) {
}
