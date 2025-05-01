package com.shop.queue.dto;

import com.shop.queue.entity.QueueRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueRequestStatusDto {
    private String requestId;
    private String userId;
    private String productId;
    private QueueRequest.QueueRequestStatus status;
    private Integer position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}

// פאיכ: queue-service/src/main/java/com/shop/queue/dto/QueueInfoDto.java
package com.shop.queue.dto;

        import lombok.AllArgsConstructor;
        import lombok.Builder;
        import lombok.Data;
        import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QueueInfoDto {
    private boolean inQueue;
    private Integer position;
    private Integer totalInQueue;
    private String requestId;
    private Integer estimatedTime;
}

// פאיכ: queue-service/src/main/java/com/shop/queue/client/ProductServiceClient.java
package com.shop.queue.client;

        import com.shop.product.dto.ProductAvailabilityDto;
        import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
        import org.springframework.cloud.openfeign.FeignClient;
        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{productId}/availability")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductAvailabilityFallback")
    ProductAvailabilityDto getProductAvailability(@PathVariable("productId") String productId);

    default ProductAvailabilityDto getProductAvailabilityFallback(String productId, Exception e) {
        throw new RuntimeException("Product service is not available", e);
    }
}
