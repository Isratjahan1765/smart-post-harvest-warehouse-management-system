package com.sphwms.product.internal.dto.responses;

import com.sphwms.product.internal.enums.ProductCategory;

public record CategorySummaryResponse(
        ProductCategory category,
        String categoryName,
        long count
) {
}
