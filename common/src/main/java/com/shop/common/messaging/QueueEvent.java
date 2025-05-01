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
public class QueueEvent {
    private String id;
    private String productId;
    private String userId;
    private String requestId;
    private QueueEventType eventType;
    private Integer position;
    private LocalDateTime timestamp;

    public enum QueueEventType {
        ADDED_TO_QUEUE,
        POSITION_UPDATED,
        READY_FOR_RESERVATION,
        REMOVED_FROM_QUEUE
    }
}