package com.sphwms.donation.internal.dto.responses;

import java.math.BigDecimal;
import java.util.List;

public record ReceiverSummaryResponse(
        BigDecimal totalKg,
        List<ReceiverItem> receivers
) {
    public record ReceiverItem(
            Long receiverId,
            String name,
            BigDecimal kg,
            int percent,
            List<String> products
    ) {}
}
