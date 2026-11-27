package com.sphwms.arrival.internal.dto.responses;

import com.sphwms.arrival.internal.enums.ArrivalGrade;
import com.sphwms.arrival.internal.enums.ArrivalStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ArrivalResponse(
        Long arrivalId,
        String batchNo,
        Long productId,
        String productName,
        Long warehouseId,
        String warehouseName,
        BigDecimal quantity,
        ArrivalGrade grade,
        LocalDate expiryDate,
        BigDecimal purchasePrice,
        BigDecimal totalCost,
        ArrivalStatus status,
        String rejectionReason,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        QualityCheckResponse qualityCheck
) {
    public record QualityCheckResponse(
            Boolean packagingOk,
            Boolean noDamage,
            Boolean quantityMatch,
            Boolean expiryValid,
            Boolean gradeMatch,
            LocalDateTime checkedAt
    ) {}
}
