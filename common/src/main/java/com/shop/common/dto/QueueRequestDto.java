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
public class QueueRequestDto {
    private String id;
    private String userId;
    private String productId;
    private Integer quantity;
    private QueueRequestStatus status;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public enum QueueRequestStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
