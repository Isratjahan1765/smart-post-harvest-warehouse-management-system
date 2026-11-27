package com.sphwms.alert.internal.dto.responses;

import com.sphwms.alert.internal.enums.AlertType;

public record AlertTypeSummaryResponse(
        AlertType type,
        String typeName,
        long count,
        int percent
) {
}
