package com.shop.queue.service;

import com.shop.common.exception.ResourceNotFoundException;
import com.shop.common.messaging.QueueEvent;
import com.shop.product.dto.ProductAvailabilityDto;
import com.shop.queue.client.ProductServiceClient;
import com.shop.queue.dto.QueueDetailDto;
import com.shop.queue.dto.QueueInfoDto;
import com.shop.queue.dto.QueueRequestStatusDto;
import com.shop.queue.entity.ProductQueue;
import com.shop.queue.entity.QueueRequest;
import com.shop.queue.repository.ProductQueueRepository;
import com.shop.queue.repository.QueueRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.shop.queue.config.RabbitMQConfig.QUEUE_EXCHANGE;
import static com.shop.queue.config.RabbitMQConfig.QUEUE_REQUEST_COMPLETED_ROUTING_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class QueueService {
    private final QueueRequestRepository queueRequestRepository;
    private final ProductQueueRepository productQueueRepository;
    private final ProductServiceClient productServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QUEUE_POSITION_PREFIX = "queue:position:";
    private static final String QUEUE_COUNT_PREFIX = "queue:count:";
    private static final int ESTIMATED_TIME_PER_REQUEST = 30; // секунд
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Transactional
    public QueueInfoDto addToQueue(String productId, String userId, Integer quantity) {
        // Перевіряємо доступність товару
        ProductAvailabilityDto availability = productServiceClient.getProductAvailability(productId);

        // Перевіряємо, чи користувач вже має запит у черзі на цей товар
        Optional<QueueRequest> existingRequest = queueRequestRepository.findByUserIdAndProductIdAndStatusIn(
                userId,
                productId,
                List.of(QueueRequest.QueueRequestStatus.PENDING, QueueRequest.QueueRequestStatus.PROCESSING));

        if (existingRequest.isPresent()) {
            QueueRequest request = existingRequest.get();

            // Повертаємо інформацію про існуючий запит
            return QueueInfoDto.builder()
                    .inQueue(true)
                    .position(request.getPosition())
                    .totalInQueue(getQueueLength(productId))
                    .requestId(request.getId())
                    .estimatedTime(calculateEstimatedTime(request.getPosition()))
                    .build();
        }

        // Створюємо чергу для товару, якщо її ще немає
        ProductQueue productQueue = getOrCreateProductQueue(productId);

        // Блокуємо чергу для операцій
        productQueue = productQueueRepository.findByProductIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product queue not found for product: " + productId));

        // Збільшуємо довжину черги
        productQueue.incrementQueueLength();
        productQueue.setAvailableStock(availability.getStockQuantity() - availability.getReservedQuantity());
        productQueueRepository.save(productQueue);

        // Створюємо новий запит у черзі
        QueueRequest queueRequest = QueueRequest.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .status(QueueRequest.QueueRequestStatus.PENDING)
                .position(productQueue.getQueueLength())
                .build();

        queueRequest = queueRequestRepository.save(queueRequest);

        // Зберігаємо позицію в Redis
        savePositionToCache(productId, queueRequest.getUserId(), queueRequest.getPosition());
        saveQueueLengthToCache(productId, productQueue.getQueueLength());

        // Публікуємо подію про додавання в чергу
        publishQueueEvent(
                QueueEvent.QueueEventType.ADDED_TO_QUEUE,
                productId,
                userId,
                queueRequest.getId(),
                queueRequest.getPosition(),
                quantity);

        log.info("Added request to queue: productId={}, userId={}, position={}",
                productId, userId, queueRequest.getPosition());

        // Повертаємо інформацію про запит
        return QueueInfoDto.builder()
                .inQueue(true)
                .position(queueRequest.getPosition())
                .totalInQueue(productQueue.getQueueLength())
                .requestId(queueRequest.getId())
                .estimatedTime(calculateEstimatedTime(queueRequest.getPosition()))
                .build();
    }

    @Transactional
    public QueueDetailDto getQueueDetails(String productId, String userId) {
        // Перевіряємо наявність черги
        ProductQueue productQueue = productQueueRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product queue not found for product: " + productId));

        // Перевіряємо доступність товару
        ProductAvailabilityDto availability = productServiceClient.getProductAvailability(productId);

        // Шукаємо запит користувача
        Optional<QueueRequest> userRequest = queueRequestRepository.findByUserIdAndProductIdAndStatusIn(
                userId,
                productId,
                List.of(QueueRequest.QueueRequestStatus.PENDING, QueueRequest.QueueRequestStatus.PROCESSING));

        Integer userPosition = null;
        String userRequestId = null;

        if (userRequest.isPresent()) {
            userPosition = userRequest.get().getPosition();
            userRequestId = userRequest.get().getId();
        }

        // Повертаємо деталі черги
        return QueueDetailDto.builder()
                .productId(productId)
                .queueLength(productQueue.getQueueLength())
                .userPosition(userPosition)
                .userRequestId(userRequestId)
                .availableStock(availability.getStockQuantity() - availability.getReservedQuantity())
                .reservedStock(availability.getReservedQuantity())
                .estimatedWaitTime(userPosition != null ? calculateEstimatedTime(userPosition) : null)
                .build();
    }

    @Transactional
    public QueueRequestStatusDto getQueueRequestStatus(String requestId) {
        QueueRequest queueRequest = queueRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue request not found: " + requestId));

        return QueueRequestStatusDto.builder()
                .requestId(queueRequest.getId())
                .userId(queueRequest.getUserId())
                .productId(queueRequest.getProductId())
                .status(queueRequest.getStatus())
                .position(queueRequest.getPosition())
                .createdAt(queueRequest.getCreatedAt())
                .updatedAt(queueRequest.getUpdatedAt())
                .completedAt(queueRequest.getCompletedAt())
                .build();
    }

    @Transactional
    @Scheduled(fixedRate = 5000) // Кожні 5 секунд
    public void processQueues() {
        log.info("Starting queue processing...");

        // Отримуємо всі активні черги
        List<ProductQueue> activeQueues = productQueueRepository.findByActive(true);

        for (ProductQueue queue : activeQueues) {
            try {
                processQueue(queue);
            } catch (Exception e) {
                log.error("Error processing queue for product {}: {}", queue.getProductId(), e.getMessage(), e);
            }
        }
    }

    @Transactional
    public void handleProductStockUpdated(String productId, Integer availableQuantity) {
        // Отримуємо чергу для товару, якщо вона є
        Optional<ProductQueue> queueOpt = productQueueRepository.findByProductId(productId);

        if (queueOpt.isPresent()) {
            ProductQueue queue = queueOpt.get();

            // Оновлюємо доступний залишок
            queue.setAvailableStock(availableQuantity);
            productQueueRepository.save(queue);

            log.info("Updated available stock for product queue {}: {}", productId, availableQuantity);
        }
    }

    private ProductQueue getOrCreateProductQueue(String productId) {
        return productQueueRepository.findByProductId(productId)
                .orElseGet(() -> {
                    // Перевіряємо доступність товару
                    ProductAvailabilityDto availability = productServiceClient.getProductAvailability(productId);

                    ProductQueue newQueue = ProductQueue.builder()
                            .id(UUID.randomUUID().toString())
                            .productId(productId)
                            .availableStock(availability.getStockQuantity() - availability.getReservedQuantity())
                            .build();

                    return productQueueRepository.save(newQueue);
                });
    }

    private void processQueue(ProductQueue queue) {
        // Отримуємо доступність товару
        ProductAvailabilityDto availability = productServiceClient.getProductAvailability(queue.getProductId());

        // Оновлюємо доступний залишок
        queue.setAvailableStock(availability.getStockQuantity() - availability.getReservedQuantity());

        // Якщо товар недоступний, пропускаємо обробку
        if (queue.getAvailableStock() <= 0) {
            log.info("No stock available for product {}, skipping queue processing", queue.getProductId());
            return;
        }

        // Отримуємо запити у статусі PENDING, відсортовані за позицією
        List<QueueRequest> pendingRequests = queueRequestRepository.findPendingRequestsByProductIdOrderByPosition(queue.getProductId());

        if (pendingRequests.isEmpty()) {
            log.info("No pending requests for product {}", queue.getProductId());
            return;
        }

        log.info("Processing {} pending requests for product {}", pendingRequests.size(), queue.getProductId());

        // Обробляємо запити
        Integer availableStock = queue.getAvailableStock();

        for (QueueRequest request : pendingRequests) {
            // Якщо товару не вистачає, припиняємо обробку
            if (availableStock < request.getQuantity()) {
                log.info("Not enough stock for request {}, breaking", request.getId());
                break;
            }

            // Змінюємо статус запиту на PROCESSING
            request.setStatus(QueueRequest.QueueRequestStatus.PROCESSING);
            queueRequestRepository.save(request);

            // Зменшуємо доступний залишок
            availableStock -= request.getQuantity();

            // Публікуємо подію про готовність до резервації
            publishQueueEvent(
                    QueueEvent.QueueEventType.READY_FOR_RESERVATION,
                    request.getProductId(),
                    request.getUserId(),
                    request.getId(),
                    request.getPosition(),
                    request.getQuantity());

            log.info("Request {} is ready for reservation", request.getId());
        }

        // Оновлюємо чергу
        productQueueRepository.save(queue);

        // Перераховуємо позиції в черзі
        recalculatePositions(queue.getProductId());
    }

    private void recalculatePositions(String productId) {
        // Отримуємо всі запити у статусі PENDING
        List<QueueRequest> pendingRequests = queueRequestRepository.findByProductIdAndStatusOrderByCreatedAtAsc(
                productId, QueueRequest.QueueRequestStatus.PENDING);

        // Перераховуємо позиції
        for (int i = 0; i < pendingRequests.size(); i++) {
            QueueRequest request = pendingRequests.get(i);
            Integer newPosition = i + 1;

            // Якщо позиція змінилася, оновлюємо запит
            if (!newPosition.equals(request.getPosition())) {
                request.setPosition(newPosition);
                queueRequestRepository.save(request);

                // Оновлюємо кеш
                savePositionToCache(productId, request.getUserId(), newPosition);

                // Публікуємо подію про оновлення позиції
                publishQueueEvent(
                        QueueEvent.QueueEventType.POSITION_UPDATED,
                        request.getProductId(),
                        request.getUserId(),
                        request.getId(),
                        newPosition,
                        request.getQuantity());

                log.info("Updated position for request {}: {}", request.getId(), newPosition);
            }
        }

        // Оновлюємо довжину черги
        Optional<ProductQueue> queueOpt = productQueueRepository.findByProductId(productId);

        if (queueOpt.isPresent()) {
            ProductQueue queue = queueOpt.get();
            Integer queueLength = pendingRequests.size();

            // Оновлюємо довжину черги
            queue.setQueueLength(queueLength);
            productQueueRepository.save(queue);

            // Оновлюємо кеш
            saveQueueLengthToCache(productId, queueLength);

            log.info("Updated queue length for product {}: {}", productId, queueLength);
        }
    }

    private void publishQueueEvent(
            QueueEvent.QueueEventType eventType,
            String productId,
            String userId,
            String requestId,
            Integer position,
            Integer quantity) {

        QueueEvent event = QueueEvent.builder()
                .id(UUID.randomUUID().toString())
                .productId(productId)
                .userId(userId)
                .requestId(requestId)
                .eventType(eventType)
                .position(position)
                .quantity(quantity)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(QUEUE_EXCHANGE, QUEUE_EXCHANGE + "." + eventType.name().toLowerCase(), event);

        // Для подій READY_FOR_RESERVATION додатково публікуємо в спеціальну чергу
        if (eventType == QueueEvent.QueueEventType.READY_FOR_RESERVATION) {
            rabbitTemplate.convertAndSend(QUEUE_EXCHANGE, QUEUE_REQUEST_COMPLETED_ROUTING_KEY, event);
        }

        log.info("Published queue event: {}", event);
    }

    private void savePositionToCache(String productId, String userId, Integer position) {
        String key = QUEUE_POSITION_PREFIX + productId + ":" + userId;
        redisTemplate.opsForValue().set(key, position, CACHE_TTL.getSeconds(), TimeUnit.SECONDS);
    }

    private void saveQueueLengthToCache(String productId, Integer queueLength) {
        String key = QUEUE_COUNT_PREFIX + productId;
        redisTemplate.opsForValue().set(key, queueLength, CACHE_TTL.getSeconds(), TimeUnit.SECONDS);
    }

    private Integer getQueueLength(String productId) {
        // Спочатку перевіряємо кеш
        String key = QUEUE_COUNT_PREFIX + productId;
        Object cachedLength = redisTemplate.opsForValue().get(key);

        if (cachedLength != null) {
            return (Integer) cachedLength;
        }

        // Якщо немає в кеші, рахуємо з БД
        Integer count = queueRequestRepository.countPendingRequestsByProductId(productId);

        // Зберігаємо в кеш
        saveQueueLengthToCache(productId, count);

        return count;
    }

    private Integer calculateEstimatedTime(Integer position) {
        // Простий розрахунок: позиція * середній час на обробку одного запиту
        return position * ESTIMATED_TIME_PER_REQUEST;
    }
}

