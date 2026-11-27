package com.sphwms.buyer.internal.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DuePaymentRequest(
        @NotNull(message = "Payment amount is required.")
        @Positive(message = "Payment amount must be positive.")
        BigDecimal amount,

        LocalDate paymentDate
) {
}
