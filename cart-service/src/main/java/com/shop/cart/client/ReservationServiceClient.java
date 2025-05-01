package com.shop.cart.client;

import com.shop.common.dto.ReservationDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "reservation-service")
public interface ReservationServiceClient {

    @PostMapping("/reservations/create")
    @CircuitBreaker(name = "reservationService", fallbackMethod = "createReservationFallback")
    ReservationDto createReservation(
            @RequestParam("productId") String productId,
            @RequestParam("userId") String userId,
            @RequestParam("quantity") Integer quantity,
            @RequestParam("cartId") String cartId);

    @PostMapping("/reservations/{reservationId}/cancel")
    @CircuitBreaker(name = "reservationService", fallbackMethod = "cancelReservationFallback")
    void cancelReservation(@PathVariable("reservationId") String reservationId);

    default ReservationDto createReservationFallback(String productId, String userId, Integer quantity, String cartId, Exception e) {
        throw new RuntimeException("Reservation service is not available", e);
    }

    default void cancelReservationFallback(String reservationId, Exception e) {
        throw new RuntimeException("Reservation service is not available", e);
    }
}
