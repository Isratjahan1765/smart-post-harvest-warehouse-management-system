package com.sphwms.product.internal.dto.responses;

import com.sphwms.product.internal.enums.ProductCategory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductResponse(
        Long productId,
        String code,
        String name,
        ProductCategory category,
        String unit,
        BigDecimal purchasePrice,
        BigDecimal sellingPrice,
        BigDecimal reorderLevel,
        BigDecimal currentStock,
        List<String> availableIn,
        LocalDate entryDate,
        LocalDate expiryDate,
        String status,
        Boolean active
) {
}
