package com.shop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemDto {
    private String id;
    private String productId;
    private ProductDto product;
    private Integer quantity;
    private BigDecimal price;
    private boolean isReserved;
    private String reservationId;
    private LocalDateTime reservationExpiresAt;
}