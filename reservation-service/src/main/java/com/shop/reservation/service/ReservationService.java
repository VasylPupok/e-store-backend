package com.shop.reservation.service;

import com.shop.common.dto.ProductDto;
import com.shop.common.dto.ReservationDto;
import com.shop.common.exception.ResourceNotFoundException;
import com.shop.common.messaging.ReservationEvent;
import com.shop.reservation.client.ProductServiceClient;
import com.shop.reservation.dto.ReservationDetailDto;
import com.shop.reservation.entity.Reservation;
import com.shop.reservation.mapper.ReservationMapper;
import com.shop.reservation.repository.ReservationRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.shop.reservation.config.RabbitMQConfig.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;
    private final ProductServiceClient productServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Value("${reservation.expiration-time-minutes:15}")
    private int reservationExpirationTimeMinutes;

    @Transactional
    public ReservationDto createReservation(String productId, String userId, Integer quantity, String cartId) {
        // ��������� ���� ����������
        Reservation reservation = Reservation.builder()
                .id(UUID.randomUUID().toString())
                .productId(productId)
                .userId(userId)
                .quantity(quantity)
                .cartId(cartId)
                .status(Reservation.ReservationStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(reservationExpirationTimeMinutes))
                .build();

        reservation = reservationRepository.save(reservation);

        // �������� ���� ��� ��������� ����������
        publishReservationEvent(
                ReservationEvent.ReservationEventType.CREATED,
                reservation.getId(),
                productId,
                userId,
                cartId,
                quantity,
                reservation.getExpiresAt());

        log.info("Created reservation: {}", reservation);

        return reservationMapper.toDto(reservation);
    }

    @Transactional
    public List<ReservationDto> getActiveReservationsByUserId(String userId) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndStatus(
                userId, Reservation.ReservationStatus.ACTIVE);

        return reservations.stream()
                .map(reservationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationDetailDto getReservationDetails(String reservationId, String userId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        // ����������, �� ���������� �������� �����������
        if (!reservation.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Reservation not found for user: " + userId);
        }

        // �������� ��� ��� �����
        ProductDto product = productServiceClient.getProduct(reservation.getProductId());

        // ����������� ���, �� ��������� �� ��������� ����������
        long timeLeftSeconds = ChronoUnit.SECONDS.between(
                LocalDateTime.now(),
                reservation.getExpiresAt());

        // ���� ��� ��� �����, ��������� 0
        timeLeftSeconds = Math.max(0, timeLeftSeconds);

        return ReservationDetailDto.builder()
                .reservation(reservationMapper.toDto(reservation))
                .product(product)
                .timeLeft((int) timeLeftSeconds)
                .cartId(reservation.getCartId())
                .build();
    }

    @Transactional
    public void cancelReservation(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        // ����������, �� ���������� �������
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Cannot cancel reservation with status: " + reservation.getStatus());
        }

        // ������� ������ ����������
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        // ��������� ���������� ������
        productServiceClient.releaseReservation(reservation.getProductId(), reservation.getQuantity());

        // �������� ���� ��� ���������� ����������
        publishReservationEvent(
                ReservationEvent.ReservationEventType.CANCELLED,
                reservation.getId(),
                reservation.getProductId(),
                reservation.getUserId(),
                reservation.getCartId(),
                reservation.getQuantity(),
                null);

        log.info("Cancelled reservation: {}", reservation);
    }

    @Transactional
    public void completeReservation(String reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));

        // ����������, �� ���������� �������
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            throw new IllegalStateException("Cannot complete reservation with status: " + reservation.getStatus());
        }

        // ������� ������ ����������
        reservation.setStatus(Reservation.ReservationStatus.COMPLETED);
        reservationRepository.save(reservation);

        // �������� ���� ��� ���������� ����������
        publishReservationEvent(
                ReservationEvent.ReservationEventType.COMPLETED,
                reservation.getId(),
                reservation.getProductId(),
                reservation.getUserId(),
                reservation.getCartId(),
                reservation.getQuantity(),
                null);

        log.info("Completed reservation: {}", reservation);
    }

    @Scheduled(fixedRate = 60000) // ����������� ����� �������
    @Transactional
    public void checkExpiredReservations() {
        log.info("Checking for expired reservations");

        // ��������� �� ���������� ����������
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations(LocalDateTime.now());

        log.info("Found {} expired reservations", expiredReservations.size());

        // ���������� ����� ����������� ����������
        for (Reservation reservation : expiredReservations) {
            try {
                // ������� ������ ����������
                reservation.setStatus(Reservation.ReservationStatus.EXPIRED);
                reservationRepository.save(reservation);

                // ��������� ���������� ������
                productServiceClient.releaseReservation(reservation.getProductId(), reservation.getQuantity());

                // �������� ���� ��� ��������� ����������
                publishReservationEvent(
                        ReservationEvent.ReservationEventType.EXPIRED,
                        reservation.getId(),
                        reservation.getProductId(),
                        reservation.getUserId(),
                        reservation.getCartId(),
                        reservation.getQuantity(),
                        null);

                log.info("Expired reservation: {}", reservation);
            } catch (Exception e) {
                log.error("Error processing expired reservation {}: {}", reservation.getId(), e.getMessage(), e);
            }
        }
    }

    private void publishReservationEvent(
            ReservationEvent.ReservationEventType eventType,
            String reservationId,
            String productId,
            String userId,
            String cartId,
            Integer quantity,
            LocalDateTime expiresAt) {

        ReservationEvent event = ReservationEvent.builder()
                .id(UUID.randomUUID().toString())
                .reservationId(reservationId)
                .productId(productId)
                .userId(userId)
                .cartId(cartId)
                .eventType(eventType)
                .quantity(quantity)
                .timestamp(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();

        // ������������� ��� routing keys � ��������� �� ���� ��䳿
        String routingKey = switch (eventType) {
            case CREATED -> RESERVATION_CREATED_ROUTING_KEY;
            case EXPIRED -> RESERVATION_EXPIRED_ROUTING_KEY;
            case COMPLETED -> RESERVATION_COMPLETED_ROUTING_KEY;
            case CANCELLED -> RESERVATION_CANCELLED_ROUTING_KEY;
        };

        rabbitTemplate.convertAndSend(RESERVATION_EXCHANGE, routingKey, event);

        log.info("Published reservation event: {}", event);
    }
}