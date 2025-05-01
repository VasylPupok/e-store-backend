package com.shop.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDto {
    private String id;
    private String userId;
    private String productId;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private ReservationStatus status;

    public enum ReservationStatus {
        ACTIVE, EXPIRED, COMPLETED
    }
}