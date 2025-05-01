package com.shop.reservation.controller;

import com.shop.common.dto.ReservationDto;
import com.shop.reservation.dto.ReservationDetailDto;
import com.shop.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "Reservation", description = "Операції з резерваціями товарів")
public class ReservationController {
    private final ReservationService reservationService;

    @GetMapping
    @Operation(summary = "Отримати всі активні резервації користувача", description = "Повертає список активних резервацій поточного користувача")
    public ResponseEntity<List<ReservationDto>> getActiveReservations(@RequestHeader("X-User-Id") String userId) {
        List<ReservationDto> reservations = reservationService.getActiveReservationsByUserId(userId);
        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    @Operation(summary = "Отримати деталі резервації", description = "Повертає детальну інформацію про резервацію")
    public ResponseEntity<ReservationDetailDto> getReservationDetails(
            @PathVariable @NotBlank String reservationId,
            @RequestHeader("X-User-Id") @NotBlank String userId) {

        ReservationDetailDto reservationDetail = reservationService.getReservationDetails(reservationId, userId);
        return ResponseEntity.ok(reservationDetail);
    }

    @PostMapping("/create")
    @Operation(summary = "Створити нову резервацію", description = "Створює нову резервацію товару")
    public ResponseEntity<ReservationDto> createReservation(
            @RequestParam @NotBlank String productId,
            @RequestParam @NotBlank String userId,
            @RequestParam @Min(1) Integer quantity,
            @RequestParam @NotBlank String cartId) {

        ReservationDto reservation = reservationService.createReservation(productId, userId, quantity, cartId);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{reservationId}/cancel")
    @Operation(summary = "Скасувати резервацію", description = "Скасовує активну резервацію та звільняє товар")
    public ResponseEntity<Void> cancelReservation(
            @PathVariable @NotBlank String reservationId) {

        reservationService.cancelReservation(reservationId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{reservationId}/complete")
    @Operation(summary = "Завершити резервацію", description = "Позначає резервацію як завершену (після покупки)")
    public ResponseEntity<Void> completeReservation(
            @PathVariable @NotBlank String reservationId) {

        reservationService.completeReservation(reservationId);
        return ResponseEntity.ok().build();
    }
}