package com.sphwms.product.internal.dto.requests;

import com.sphwms.product.internal.enums.ProductCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductRequest(
        @NotBlank(message = "Required fields are missing.")
        String name,

        @NotNull(message = "Required fields are missing.")
        ProductCategory category,

        @NotNull(message = "Invalid price - enter a positive number.")
        @Positive(message = "Invalid price - enter a positive number.")
        BigDecimal purchasePrice,

        @NotNull(message = "Invalid price - enter a positive number.")
        @Positive(message = "Invalid price - enter a positive number.")
        BigDecimal sellingPrice,

        BigDecimal reorderLevel,

        Long warehouseId,
        BigDecimal quantity,
        LocalDate entryDate,
        LocalDate expiryDate
) {
}
