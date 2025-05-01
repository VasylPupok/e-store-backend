package com.shop.cart.entity;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {
    private String id;
    private String productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private boolean isReserved;
    private String reservationId;
    private LocalDateTime reservationExpiresAt;
    private boolean limitedProduct;
}