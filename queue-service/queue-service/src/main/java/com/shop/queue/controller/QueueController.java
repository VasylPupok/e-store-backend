package com.shop.queue.controller;

import com.shop.queue.dto.QueueDetailDto;
import com.shop.queue.dto.QueueInfoDto;
import com.shop.queue.dto.QueueRequestStatusDto;
import com.shop.queue.service.QueueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Queue", description = "Операції з чергами на товари")
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/add")
    @Operation(summary = "Додати запит в чергу", description = "Додає запит користувача в чергу на товар")
    public ResponseEntity<QueueInfoDto> addToQueue(
            @RequestParam @NotBlank String productId,
            @RequestParam @NotBlank String userId,
            @RequestParam @Min(1) Integer quantity) {

        QueueInfoDto queueInfo = queueService.addToQueue(productId, userId, quantity);
        return ResponseEntity.ok(queueInfo);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Отримати інформацію про чергу на товар", description = "Повертає поточний стан черги на товар та позицію користувача в ній")
    public ResponseEntity<QueueDetailDto> getQueueDetails(
            @PathVariable @NotBlank String productId,
            @RequestHeader("X-User-Id") @NotBlank String userId) {

        QueueDetailDto queueDetail = queueService.getQueueDetails(productId, userId);
        return ResponseEntity.ok(queueDetail);
    }

    @GetMapping("/requests/{requestId}")
    @Operation(summary = "Отримати статус запиту в черзі", description = "Повертає поточний статус запиту користувача в черзі")
    public ResponseEntity<QueueRequestStatusDto> getQueueRequestStatus(
            @PathVariable @NotBlank String requestId) {

        QueueRequestStatusDto requestStatus = queueService.getQueueRequestStatus(requestId);
        return ResponseEntity.ok(requestStatus);
    }
}
