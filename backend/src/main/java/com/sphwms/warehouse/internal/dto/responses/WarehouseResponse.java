package com.sphwms.warehouse.internal.dto.responses;

import java.math.BigDecimal;
import java.util.List;

public record WarehouseResponse(
        Long warehouseId,
        Integer codeNumber,
        String code,
        String name,
        String location,
        String inCharge,
        BigDecimal capacityKg,
        BigDecimal usedKg,
        BigDecimal freeKg,
        Integer usedPercent,
        BigDecimal stockAmount,
        String status,
        BigDecimal totalStock,
        List<StockBreakdownItem> stockBreakdown
) {
    public record StockBreakdownItem(Long productId, String productName, BigDecimal quantity) {}
}
