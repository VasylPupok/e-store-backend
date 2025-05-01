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
public class ReservationEvent {
    private String id;
    private String reservationId;
    private String productId;
    private String userId;
    private String cartId;
    private ReservationEventType eventType;
    private Integer quantity;
    private LocalDateTime timestamp;
    private LocalDateTime expiresAt;

    public enum ReservationEventType {
        CREATED,
        EXPIRED,
        COMPLETED,
        CANCELLED
    }
}