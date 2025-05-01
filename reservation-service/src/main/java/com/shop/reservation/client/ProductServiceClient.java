package com.shop.reservation.client;

import com.shop.common.dto.ProductDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "product-service")
public interface ProductServiceClient {

    @GetMapping("/products/{productId}")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductFallback")
    ProductDto getProduct(@PathVariable("productId") String productId);

    @PostMapping("/products/{productId}/release")
    @CircuitBreaker(name = "productService", fallbackMethod = "releaseReservationFallback")
    void releaseReservation(
            @PathVariable("productId") String productId,
            @RequestParam("quantity") Integer quantity);

    default ProductDto getProductFallback(String productId, Exception e) {
        throw new RuntimeException("Product service is not available", e);
    }

    default void releaseReservationFallback(String productId, Integer quantity, Exception e) {
        throw new RuntimeException("Product service is not available", e);
    }
}