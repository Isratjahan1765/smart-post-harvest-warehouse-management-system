package com.sphwms.donation.internal.dto.requests;

import com.sphwms.donation.internal.enums.ReceiverType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReceiverRequest(
        @NotBlank(message = "Required receiver information is missing.")
        String name,

        String contactPerson,

        @NotBlank(message = "Required receiver information is missing.")
        String phone,

        @NotBlank(message = "Required receiver information is missing.")
        String address,

        @NotNull(message = "Required receiver information is missing.")
        ReceiverType type
) {
}
