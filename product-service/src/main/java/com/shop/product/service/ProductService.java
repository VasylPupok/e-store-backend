package com.shop.product.service;

import com.shop.common.dto.ProductDto;
import com.shop.common.exception.InsufficientStockException;
import com.shop.common.exception.ResourceNotFoundException;
import com.shop.common.messaging.ProductEvent;
import com.shop.product.dto.ProductAvailabilityDto;
import com.shop.product.entity.Product;
import com.shop.product.mapper.ProductMapper;
import com.shop.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.shop.product.config.RabbitMQConfig.PRODUCT_EXCHANGE;
import static com.shop.product.config.RabbitMQConfig.STOCK_UPDATE_ROUTING_KEY;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String QUEUE_COUNT_KEY_PREFIX = "queue:count:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    @Transactional(readOnly = true)
    public Page<ProductDto> findAll(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products;

        if (category != null && !category.isEmpty()) {
            products = productRepository.findByCategory(category, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return products.map(productMapper::toDto);
    }

    @Transactional(readOnly = true)
    public ProductDto findById(String id) {
        Product product = findProductById(id);
        return productMapper.toDto(product);
    }

    @Transactional
    public ProductDto create(ProductDto productDto) {
        Product product = productMapper.toEntity(productDto);
        product = productRepository.save(product);

        // ѕубл≥куЇмо под≥ю про оновленн€ товару
        publishStockUpdateEvent(product);

        return productMapper.toDto(product);
    }

    @Transactional
    public ProductDto update(String id, ProductDto productDto) {
        Product existingProduct = findProductById(id);

        existingProduct.setName(productDto.getName());
        existingProduct.setPrice(productDto.getPrice());
        existingProduct.setCategory(productDto.getCategory());
        existingProduct.setImage(productDto.getImage());
        existingProduct.setLimited(productDto.isLimited());

        // якщо зм≥нилась к≥льк≥сть товару, публ≥куЇмо под≥ю
        if (!existingProduct.getStockQuantity().equals(productDto.getStockQuantity())) {
            existingProduct.setStockQuantity(productDto.getStockQuantity());
            publishStockUpdateEvent(existingProduct);
        }

        existingProduct = productRepository.save(existingProduct);
        return productMapper.toDto(existingProduct);
    }

    @Transactional(readOnly = true)
    public ProductAvailabilityDto checkAvailability(String productId) {
        Product product = findProductById(productId);

        // ќтримуЇмо к≥льк≥сть запит≥в у черз≥ з Redis
        Integer inQueueRequests = getQueueCount(productId);

        return ProductAvailabilityDto.builder()
                .productId(product.getId())
                .available(product.getAvailableQuantity() > 0)
                .stockQuantity(product.getStockQuantity())
                .reservedQuantity(product.getReservedQuantity())
                .inQueueRequests(inQueueRequests)
                .build();
    }

    @Transactional
    public void reserveProduct(String productId, Integer quantity, String userId) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        if (product.getAvailableQuantity() < quantity) {
            throw new InsufficientStockException("Not enough stock available for product: " + productId);
        }

        // «б≥льшуЇмо к≥льк≥сть зарезервованого товару
        product.setReservedQuantity(product.getReservedQuantity() + quantity);
        productRepository.save(product);

        // ѕубл≥куЇмо под≥ю про резервац≥ю товару
        publishProductReservedEvent(product, quantity, userId);

        log.info("Reserved {} units of product {} for user {}", quantity, productId, userId);
    }

    @Transactional
    public void releaseReservation(String productId, Integer quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        // «меншуЇмо к≥льк≥сть зарезервованого товару
        int newReservedQuantity = Math.max(0, product.getReservedQuantity() - quantity);
        product.setReservedQuantity(newReservedQuantity);
        productRepository.save(product);

        // ѕубл≥куЇмо под≥ю про оновленн€ товару
        publishStockUpdateEvent(product);

        log.info("Released reservation of {} units for product {}", quantity, productId);
    }

    private Product findProductById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
    }

    private void publishStockUpdateEvent(Product product) {
        ProductEvent event = ProductEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .productId(product.getId())
                .eventType(ProductEvent.ProductEventType.STOCK_UPDATED)
                .quantity(product.getAvailableQuantity())
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(PRODUCT_EXCHANGE, STOCK_UPDATE_ROUTING_KEY, event);
        log.info("Published stock update event for product {}: available quantity = {}",
                product.getId(), product.getAvailableQuantity());
    }

    private void publishProductReservedEvent(Product product, Integer quantity, String userId) {
        ProductEvent event = ProductEvent.builder()
                .id(java.util.UUID.randomUUID().toString())
                .productId(product.getId())
                .eventType(ProductEvent.ProductEventType.PRODUCT_RESERVED)
                .quantity(quantity)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        rabbitTemplate.convertAndSend(PRODUCT_EXCHANGE, PRODUCT_EXCHANGE + ".reserved", event);
        log.info("Published product reserved event for product {}, quantity {}, user {}",
                product.getId(), quantity, userId);
    }

    private Integer getQueueCount(String productId) {
        String key = QUEUE_COUNT_KEY_PREFIX + productId;
        Object count = redisTemplate.opsForValue().get(key);
        return count != null ? (Integer) count : 0;
    }

    public void updateQueueCount(String productId, Integer count) {
        String key = QUEUE_COUNT_KEY_PREFIX + productId;
        redisTemplate.opsForValue().set(key, count, CACHE_TTL);
    }
}
