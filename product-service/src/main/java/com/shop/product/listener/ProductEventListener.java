package com.shop.product.service;

import com.shop.common.messaging.ReservationEvent;
import com.shop.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.shop.product.config.RabbitMQConfig.RESERVATION_EXPIRED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventListener {
    private final ProductService productService;

    @RabbitListener(queues = RESERVATION_EXPIRED_QUEUE)
    public void handleReservationExpiredEvent(ReservationEvent event) {
        if (event.getEventType() == ReservationEvent.ReservationEventType.EXPIRED) {
            log.info("Received reservation expired event: {}", event);

            // Звільняємо резервацію у товарі
            productService.releaseReservation(event.getProductId(), event.getQuantity());
        }
    }
}