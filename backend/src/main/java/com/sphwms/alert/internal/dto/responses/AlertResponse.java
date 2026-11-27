package com.sphwms.alert.internal.dto.responses;

import com.sphwms.alert.internal.enums.AlertEntityType;
import com.sphwms.alert.internal.enums.AlertType;
import java.time.LocalDateTime;

public record AlertResponse(
        Long alertId,
        AlertType type,
        AlertEntityType entityType,
        Long entityId,
        Long warehouseId,
        String warehouseName,
        String itemName,
        String details,
        String dedupeKey,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
}
