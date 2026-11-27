package com.sphwms.sale.internal.dto.responses;

import com.sphwms.sale.internal.enums.PaymentStatus;
import com.sphwms.sale.internal.enums.SaleStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SaleResponse(
        Long saleId,
        String saleNo,
        Long buyerId,
        String buyerName,
        Long productId,
        String productName,
        BigDecimal quantity,
        BigDecimal rate,
        BigDecimal purchasePrice,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal dueAmount,
        PaymentStatus paymentStatus,
        SaleStatus status,
        LocalDate saleDate,
        LocalDate dueDate,
        LocalDateTime createdAt
) {
}
