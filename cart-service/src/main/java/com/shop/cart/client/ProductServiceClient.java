package com.shop.cart.client;

import com.shop.common.dto.ProductDto;
import com.shop.product.dto.ProductAvailabilityDto;
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

    @GetMapping("/products/{productId}/availability")
    @CircuitBreaker(name = "productService", fallbackMethod = "getProductAvailabilityFallback")
    ProductAvailabilityDto getProductAvailability(@PathVariable("productId") String productId);

    @PostMapping("/products/{productId}/reserve")
    @CircuitBreaker(name = "productService", fallbackMethod = "reserveProductFallback")
    void reserveProduct(
            @PathVariable("productId") String productId,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("userId") String userId);

    @PostMapping("/products/{productId}/release")
    @CircuitBreaker(name = "productService", fallbackMethod = "releaseReservationFallback")
    void releaseReservation(
            @PathVariable("productId") String productId,
            @RequestParam("quantity") Integer quantity);

    default ProductDto getProductFallback(String productId, Exception e) {
        throw new RuntimeException("Product service is not available", e);
    }

    default ProductAvailabilityDto getProductAvailabilityFallback(String productId, Exception e) {
        throw new RuntimeException("Product service is not available", e);
    }

    default void reserveProductFallback(String productId, Integer quantity, String userId, Exception e) {
        throw new RuntimeException("Product service is not available", e);
    }

    default void releaseReservationFallback(String productId, Integer quantity, Exception e) {
        throw new RuntimeException("Product service is not available", e);
    }
}