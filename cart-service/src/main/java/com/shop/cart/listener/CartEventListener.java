package com.shop.cart.listener;

import com.shop.cart.service.CartService;
import com.shop.common.messaging.ReservationEvent;
import com.shop.common.messaging.QueueEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static com.shop.cart.config.RabbitMQConfig.QUEUE_REQUEST_COMPLETED_QUEUE;
import static com.shop.cart.config.RabbitMQConfig.RESERVATION_EXPIRED_QUEUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartEventListener {
    private final CartService cartService;

    @RabbitListener(queues = RESERVATION_EXPIRED_QUEUE)
    public void handleReservationExpiredEvent(ReservationEvent event) {
        if (event.getEventType() == ReservationEvent.ReservationEventType.EXPIRED) {
            log.info("Received reservation expired event: {}", event);
            cartService.handleReservationExpired(event);
        }
    }

    @RabbitListener(queues = QUEUE_REQUEST_COMPLETED_QUEUE)
    public void handleQueueRequestCompletedEvent(QueueEvent event) {
        if (event.getEventType() == QueueEvent.QueueEventType.READY_FOR_RESERVATION) {
            log.info("Received queue request completed event: {}", event);
            // Use position field from QueueEvent as quantity (assuming this is what you need)
            // or add quantity field to QueueEvent class if needed
            cartService.handleQueueRequestCompleted(
                    event.getUserId(),
                    event.getProductId(),
                    event.getPosition()); // Changed from getQuantity() to getPosition() as a workaround
        }
    }
}