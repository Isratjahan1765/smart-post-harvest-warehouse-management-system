package com.sphwms.arrival.internal.dto.requests;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank(message = "Rejection reason is required.")
        String reason
) {
}
