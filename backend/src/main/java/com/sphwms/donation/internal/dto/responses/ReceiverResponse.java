package com.sphwms.donation.internal.dto.responses;

import com.sphwms.donation.internal.enums.ReceiverType;

public record ReceiverResponse(
        Long receiverId,
        String name,
        String contactPerson,
        String phone,
        String address,
        ReceiverType type,
        String status
) {
}
