package com.sphwms.buyer.internal.dto.responses;

import com.sphwms.buyer.internal.enums.BuyerType;
import java.math.BigDecimal;
import java.time.LocalDate;

public record BuyerResponse(
        Long buyerId,
        String name,
        String phone,
        String address,
        BuyerType buyerType,
        String status,
        BigDecimal totalSales,
        BigDecimal totalPurchase,
        BigDecimal totalPaid,
        BigDecimal totalDue,
        LocalDate lastPurchase
) {
}
