package com.sphwms.arrival.internal.dto.responses;

public record ArrivalSummaryResponse(
        long total,
        long pending,
        long confirmed,
        long rejected
) {
}
