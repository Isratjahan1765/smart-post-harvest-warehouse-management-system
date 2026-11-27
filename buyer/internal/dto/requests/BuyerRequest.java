package com.sphwms.buyer.internal.dto.requests;

import com.sphwms.buyer.internal.enums.BuyerType;
import jakarta.validation.constraints.NotBlank;

public record BuyerRequest(
        @NotBlank(message = "Required buyer information is missing.")
        String name,

        @NotBlank(message = "Required buyer information is missing.")
        String phone,

        @NotBlank(message = "Required buyer information is missing.")
        String address,

        BuyerType buyerType
) {
}
