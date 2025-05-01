package com.shop.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartResponse {
    private String cartId;
    private boolean added;
    private QueueInfo queueInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QueueInfo {
        private boolean inQueue;
        private Integer position;
        private Integer totalInQueue;
        private String requestId;
        private Integer estimatedTime;
    }
}