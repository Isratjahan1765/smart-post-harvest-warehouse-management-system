package com.sphwms.warehouse.internal.dto.responses;

public record WarehouseOptionResponse(
        Long warehouseId,
        String code,
        String name,
        boolean active
) {
}
