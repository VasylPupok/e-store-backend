package com.shop.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAvailabilityDto {
    private String productId;
    private boolean available;
    private Integer stockQuantity;
    private Integer reservedQuantity;
    private Integer inQueueRequests;
}
