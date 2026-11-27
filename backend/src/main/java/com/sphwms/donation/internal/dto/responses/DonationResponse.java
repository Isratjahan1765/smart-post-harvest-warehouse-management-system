package com.sphwms.donation.internal.dto.responses;

import com.sphwms.donation.internal.enums.DonationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DonationResponse(
        Long donationId,
        String donationNo,
        Long receiverId,
        String receiverName,
        Long productId,
        String productName,
        String batchNo,
        String warehouseName,
        BigDecimal quantity,
        BigDecimal purchasePrice,
        BigDecimal totalValue,
        String conditionNote,
        DonationStatus status,
        LocalDate donationDate,
        LocalDateTime createdAt
) {
}
