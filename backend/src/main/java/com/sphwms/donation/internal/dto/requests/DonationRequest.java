package com.sphwms.donation.internal.dto.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record DonationRequest(
        @NotNull(message = "Required fields are missing.")
        Long receiverId,

        @NotNull(message = "Required fields are missing.")
        Long batchId,

        @NotNull(message = "Required fields are missing.")
        @Positive(message = "Invalid quantity - enter a positive number.")
        BigDecimal quantity,

        String conditionNote,
        Boolean safeConfirmed,
        LocalDate donationDate
) {
}
