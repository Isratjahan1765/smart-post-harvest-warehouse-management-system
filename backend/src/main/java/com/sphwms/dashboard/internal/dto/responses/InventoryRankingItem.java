package com.sphwms.dashboard.internal.dto.responses;

import java.math.BigDecimal;

public record InventoryRankingItem(
        Long productId,
        String productName,
        String category,
        BigDecimal quantityKg,
        BigDecimal totalQty,
        BigDecimal inventoryValue,
        BigDecimal totalValue
) {
    public InventoryRankingItem(Long productId, String productName, String category, BigDecimal qty, BigDecimal val) {
        this(productId, productName, category, qty, qty, val, val);
    }
}
