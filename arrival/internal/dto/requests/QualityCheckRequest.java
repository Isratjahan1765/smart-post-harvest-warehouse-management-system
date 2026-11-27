package com.sphwms.arrival.internal.dto.requests;

public record QualityCheckRequest(
        Boolean packagingOk,
        Boolean noDamage,
        Boolean quantityMatch,
        Boolean expiryValid,
        Boolean gradeMatch
) {
}
