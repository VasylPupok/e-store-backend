package com.shop.common.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductEvent {
    private String id;
    private String productId;
    private ProductEventType eventType;
    private Integer quantity;
    private String userId;
    private LocalDateTime timestamp;

    public enum ProductEventType {
        STOCK_UPDATED,
        PRODUCT_RESERVED,
        RESERVATION_EXPIRED,
        PRODUCT_SOLD
    }
}
