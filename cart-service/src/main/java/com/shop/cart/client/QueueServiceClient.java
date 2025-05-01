package com.shop.cart.client;

import com.shop.cart.dto.AddToCartResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "queue-service")
public interface QueueServiceClient {

    @PostMapping("/queues/add")
    @CircuitBreaker(name = "queueService", fallbackMethod = "addToQueueFallback")
    AddToCartResponse.QueueInfo addToQueue(
            @RequestParam("productId") String productId,
            @RequestParam("userId") String userId,
            @RequestParam("quantity") Integer quantity);

    default AddToCartResponse.QueueInfo addToQueueFallback(String productId, String userId, Integer quantity, Exception e) {
        throw new RuntimeException("Queue service is not available", e);
    }
}
